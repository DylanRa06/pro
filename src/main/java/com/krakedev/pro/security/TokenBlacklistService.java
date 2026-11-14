package com.krakedev.pro.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.krakedev.pro.model.TokenRevocado;
import com.krakedev.pro.repository.TokenRevocadoRepository;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    private final TokenRevocadoRepository repository;
    public TokenBlacklistService(TokenRevocadoRepository repository) { this.repository = repository; }
    public boolean estaRevocado(String jti) { return repository.existsById(jti); }
    public void revocar(DecodedJWT token) {
        repository.saveAndFlush(new TokenRevocado(token.getId(), token.getExpiresAtAsInstant()));
    }
}
