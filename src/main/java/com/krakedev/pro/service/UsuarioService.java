package com.krakedev.pro.service;

import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.krakedev.pro.model.Usuario;
import com.krakedev.pro.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public Usuario registrar(String username, String password) {
        if (username == null || username.trim().isEmpty() || username.trim().length() > 50
                || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usuario y contraseña son obligatorios; usuario máximo 50 caracteres y clave máximo 72 bytes");
        }
        String nombre = username.trim();
        if (repository.findByUsername(nombre).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(nombre);
        usuario.setPassword(encoder.encode(password));
        usuario.setRol("USER");
        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
    }

    public Usuario autenticar(String username, String password) {
        if (username == null || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw noAutorizado();
        }
        Usuario usuario = repository.findByUsername(username.trim()).orElseThrow(this::noAutorizado);
        if (!encoder.matches(password, usuario.getPassword())) {
            throw noAutorizado();
        }
        return usuario;
    }

    private ResponseStatusException noAutorizado() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }
}
