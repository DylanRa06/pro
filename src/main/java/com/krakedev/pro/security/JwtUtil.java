package com.krakedev.pro.security;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.krakedev.pro.model.Usuario;

@Component
public class JwtUtil {
    private final Algorithm algoritmo;
    private final long minutos;
    public JwtUtil(@Value("${app.jwt.secret:}") String secreto,
            @Value("${app.jwt.expiration-minutes:30}") long minutos) {
        if (secreto.isBlank()) {
            byte[] clave = new byte[32];
            new SecureRandom().nextBytes(clave);
            this.algoritmo = Algorithm.HMAC256(clave);
        } else {
            if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes");
            }
            this.algoritmo = Algorithm.HMAC256(secreto);
        }
        this.minutos = minutos;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return JWT.create().withIssuer("patitas-rescate")
                .withSubject(usuario.getUsername())
                .withClaim("rol", usuario.getRol())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(ahora)
                .withExpiresAt(ahora.plus(minutos, ChronoUnit.MINUTES))
                .sign(algoritmo);
    }
}
