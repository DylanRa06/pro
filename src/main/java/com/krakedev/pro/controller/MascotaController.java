package com.krakedev.pro.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.krakedev.pro.model.Mascota;
import com.krakedev.pro.service.MascotaService;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {
    private final MascotaService service;
    public MascotaController(MascotaService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Mascota> registrar(@RequestBody Mascota mascota) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(mascota));
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Mascota> disponibles() { return service.listarDisponibles(); }

    @PutMapping("/{id}/adoptar")
    @PreAuthorize("hasRole('ADMIN')")
    public Mascota adoptar(@PathVariable Long id) { return service.adoptar(id); }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Mascota> listar() { return service.listar(); }
}
