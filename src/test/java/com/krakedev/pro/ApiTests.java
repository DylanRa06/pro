package com.krakedev.pro;

import java.time.Instant;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krakedev.pro.repository.MascotaRepository;
import com.krakedev.pro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MascotaRepository mascotas;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired com.krakedev.pro.security.JwtUtil jwt;
    @Autowired com.krakedev.pro.repository.TokenRevocadoRepository revocados;
    private String adminToken;

    @BeforeEach void limpiar() {
        revocados.deleteAll(); mascotas.deleteAll(); usuarios.deleteAll();
        var admin = new com.krakedev.pro.model.Usuario();
        admin.setUsername("voluntario_test"); admin.setRol("ADMIN");
        adminToken = jwt.generarToken(admin);
    }

    @Test void mascotasSePersistenSeRecortanYSiempreSonDisponibles() throws Exception {
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .content("""
                {"id":99,"nombre":"  Luna  ","especie":"  Perro ","edad":2,"estado":"ADOPTADA"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Luna"))
                .andExpect(jsonPath("$.especie").value("Perro"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .content("""
                {"nombre":"Milo","especie":"Gato","edad":1}
                """)).andExpect(status().isCreated());
        assertEquals(2, mascotas.count());
        mvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test void rechazaMascotasInvalidasSinPersistir() throws Exception {
        for (String body : new String[]{
                "{\"nombre\":\" \",\"especie\":\"Perro\",\"edad\":2}",
                "{\"nombre\":\"Luna\",\"especie\":\" \",\"edad\":2}",
                "{\"nombre\":\"Luna\",\"especie\":\"Perro\",\"edad\":-1}",
                "{\"edad\":1}", "json incorrecto"}) {
            mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        }
        assertEquals(0, mascotas.count());
    }

    @Test void registroProtegeClaveYNoPermiteElegirRol() throws Exception {
        registrar();
        var u = usuarios.findByUsername("adoptante_demo").orElseThrow();
        assertTrue(u.getPassword().startsWith("$2a$10$"));
        assertTrue(encoder.matches("ClaveDemo2026!", u.getPassword()));
        assertEquals("USER", u.getRol());
        mvc.perform(post("/api/auth/registrar").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"OtraClave"}
                """)).andExpect(status().isConflict());
        assertEquals(1, usuarios.count());
    }

    @Test void registroRechazaCamposVacios() throws Exception {
        for (String body : new String[]{
                "{\"username\":\" \",\"password\":\"clave\"}",
                "{\"username\":\"usuario\",\"password\":\" \"}", "{}"}) {
            mvc.perform(post("/api/auth/registrar").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, usuarios.count());
    }

    @Test void loginEmiteTokenFirmadoYFallaSinRevelarLaCausa() throws Exception {
        registrar();
        var respuesta = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"ClaveDemo2026!"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(respuesta).get("token").asText();
        var decoded = JWT.require(Algorithm.HMAC256("secreto-exclusivo-de-pruebas-automaticas-32"))
                .withIssuer("patitas-rescate").build().verify(token);
        assertEquals("adoptante_demo", decoded.getSubject());
        assertEquals("USER", decoded.getClaim("rol").asString());
        assertTrue(decoded.getExpiresAtAsInstant().isAfter(Instant.now()));
        assertFalse(decoded.getClaims().containsKey("password"));
        String anterior = null;
        for (String body : new String[]{
                "{\"username\":\"adoptante_demo\",\"password\":\"incorrecta\"}",
                "{\"username\":\"noexiste\",\"password\":\"incorrecta\"}"}) {
            String error = mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.token").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            if (anterior != null) assertEquals(anterior, error);
            anterior = error;
        }
    }


    private String loginUsuario() throws Exception {
        registrar();
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"adoptante_demo\",\"password\":\"ClaveDemo2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test void usuarioConsultaPeroNoRegistraMascotas() throws Exception {
        String token = loginUsuario();
        mvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("adoptante_demo"))
                .andExpect(jsonPath("$.rol").value("USER"));
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"nombre\":\"Luna\",\"especie\":\"Perro\",\"edad\":2}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").exists());
        assertEquals(0, mascotas.count());
    }

    @Test void logoutRevocaSoloEseTokenYPermiteNuevoLogin() throws Exception {
        String token = loginUsuario();
        var u = usuarios.findByUsername("adoptante_demo").orElseThrow();
        String otro = jwt.generarToken(u);
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertEquals(1, revocados.count());
        for (String ruta : new String[]{"/api/auth/perfil", "/api/mascotas"}) {
            mvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + otro))
                .andExpect(status().isOk());
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"adoptante_demo\",\"password\":\"ClaveDemo2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + json.readTree(body).get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test void rutasProtegidasRechazanAusenciaDeToken() throws Exception {
        mvc.perform(get("/api/auth/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test void rechazaTokensAlteradosExpiradosYOtroEmisor() throws Exception {
        var algoritmo = Algorithm.HMAC256("secreto-exclusivo-de-pruebas-automaticas-32");
        String alterado = (adminToken.charAt(0) == 'a' ? "b" : "a") + adminToken.substring(1);
        String expirado = JWT.create().withIssuer("patitas-rescate").withSubject("usuario")
                .withClaim("rol", "USER").withJWTId("vencido")
                .withExpiresAt(Instant.now().minusSeconds(60)).sign(algoritmo);
        String otroEmisor = JWT.create().withIssuer("otro").withSubject("usuario")
                .withClaim("rol", "ADMIN").withJWTId("otro")
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(algoritmo);
        String sinExpiracion = JWT.create().withIssuer("patitas-rescate").withSubject("usuario")
                .withClaim("rol", "ADMIN").withJWTId("sin-exp").sign(algoritmo);
        String firmaFalsa = JWT.create().withIssuer("patitas-rescate").withSubject("usuario")
                .withClaim("rol", "ADMIN").withJWTId("falso").withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.HMAC256("otra-clave-que-no-es-la-del-servidor"));
        for (String header : new String[]{"Bearer " + alterado, "Bearer " + expirado,
                "Bearer " + otroEmisor, "Bearer " + sinExpiracion, "Bearer " + firmaFalsa,
                "Bearer basura", "Bearer ", "Basic abc"}) {
            mvc.perform(get("/api/auth/perfil").header("Authorization", header))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensaje").exists());
        }
    }

    private void registrar() throws Exception {
        mvc.perform(post("/api/auth/registrar").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"ClaveDemo2026!","rol":"ADMIN"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
