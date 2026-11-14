package com.krakedev.pro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado {
    @Id
    private String jti;
    private Instant expiracion;
    public TokenRevocado() {}
    public TokenRevocado(String jti, Instant expiracion) {
        this.jti = jti;
        this.expiracion = expiracion;
    }
    public String getJti() { return jti; }
    public Instant getExpiracion() { return expiracion; }
}
