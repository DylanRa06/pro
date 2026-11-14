package com.krakedev.pro.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.krakedev.pro.model.Usuario;
import com.krakedev.pro.security.JwtUtil;
import com.krakedev.pro.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService service;
    private final JwtUtil jwt;
    public AuthController(UsuarioService service, JwtUtil jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    public record Credenciales(String username, String password) {}

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Credenciales datos) {
        Usuario usuario = service.registrar(datos.username(), datos.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", usuario.getId(), "username", usuario.getUsername(), "rol", usuario.getRol()));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Credenciales datos) {
        Usuario usuario = service.autenticar(datos.username(), datos.password());
        return Map.of("token", jwt.generarToken(usuario));
    }
}
