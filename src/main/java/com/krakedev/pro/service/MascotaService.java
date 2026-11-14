package com.krakedev.pro.service;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.krakedev.pro.model.Mascota;
import com.krakedev.pro.repository.MascotaRepository;

@Service
public class MascotaService {
    private final MascotaRepository repository;
    public MascotaService(MascotaRepository repository) { this.repository = repository; }

    public Mascota registrar(Mascota datos) {
        if (datos.getNombre() == null || datos.getNombre().trim().isEmpty()
                || datos.getEspecie() == null || datos.getEspecie().trim().isEmpty()
                || datos.getEdad() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre y especie son obligatorios; edad no puede ser negativa");
        }
        Mascota mascota = new Mascota();
        mascota.setNombre(datos.getNombre().trim());
        mascota.setEspecie(datos.getEspecie().trim());
        mascota.setEdad(datos.getEdad());
        mascota.setEstado("DISPONIBLE");
        return repository.save(mascota);
    }

    public List<Mascota> listar() { return repository.findAll(); }
}
