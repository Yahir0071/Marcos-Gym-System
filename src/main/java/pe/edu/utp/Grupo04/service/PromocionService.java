package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Promocion;

import java.util.List;
import java.util.Optional;

public interface PromocionService {
    List<Promocion> listarVigentes();
    List<Promocion> listarTodas();
    Optional<Promocion> buscarPorId(Long id);
    Promocion guardar(Promocion promocion);
    void cambiarEstado(Long id, boolean activa);
}
