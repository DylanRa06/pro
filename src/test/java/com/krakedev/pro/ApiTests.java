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

    @BeforeEach void limpiar() { mascotas.deleteAll(); usuarios.deleteAll(); }

    @Test void mascotasSePersistenSeRecortanYSiempreSonDisponibles() throws Exception {
        mvc.perform(post("/api/mascotas").contentType("application/json")
                .content("""
                {"id":99,"nombre":"  Luna  ","especie":"  Perro ","edad":2,"estado":"ADOPTADA"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Luna"))
                .andExpect(jsonPath("$.especie").value("Perro"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        mvc.perform(post("/api/mascotas").contentType("application/json")
                .content("""
                {"nombre":"Milo","especie":"Gato","edad":1}
                """)).andExpect(status().isCreated());
        assertEquals(2, mascotas.count());
        mvc.perform(get("/api/mascotas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test void rechazaMascotasInvalidasSinPersistir() throws Exception {
        for (String body : new String[]{
                "{\"nombre\":\" \",\"especie\":\"Perro\",\"edad\":2}",
                "{\"nombre\":\"Luna\",\"especie\":\" \",\"edad\":2}",
                "{\"nombre\":\"Luna\",\"especie\":\"Perro\",\"edad\":-1}",
                "{\"edad\":1}", "json incorrecto"}) {
            mvc.perform(post("/api/mascotas").contentType("application/json").content(body))
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

    private void registrar() throws Exception {
        mvc.perform(post("/api/auth/registrar").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"ClaveDemo2026!","rol":"ADMIN"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
