package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.exception.ValidacionNegocioException;
import pe.edu.utp.Grupo04.model.Promocion;
import pe.edu.utp.Grupo04.repository.PromocionRepository;
import pe.edu.utp.Grupo04.service.PromocionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromocionServiceImpl implements PromocionService {

    private final PromocionRepository promocionRepository;

    public PromocionServiceImpl(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promocion> listarVigentes() {
        LocalDate hoy = LocalDate.now();
        return promocionRepository.findByActivaTrue().stream()
                .filter(p -> (p.getFechaInicio() == null || !p.getFechaInicio().isAfter(hoy)) &&
                             (p.getFechaFin() == null || !p.getFechaFin().isBefore(hoy)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promocion> listarTodas() {
        return promocionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promocion> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return promocionRepository.findById(id);
    }

    @Override
    public Promocion guardar(Promocion promocion) {
        if (promocion.getFechaInicio() != null && promocion.getFechaFin() != null) {
            if (promocion.getFechaFin().isBefore(promocion.getFechaInicio())) {
                throw new ValidacionNegocioException("La fecha de fin de la promoción debe ser posterior o igual a la fecha de inicio.");
            }
        }
        if (promocion.getPorcentajeDescuento() != null) {
            if (promocion.getPorcentajeDescuento() <= 0 || promocion.getPorcentajeDescuento() > 100) {
                throw new ValidacionNegocioException("El porcentaje de descuento debe encontrarse estrictamente entre 1% y 100%.");
            }
        }
        return promocionRepository.save(promocion);
    }

    @Override
    public void cambiarEstado(Long id, boolean activa) {
        Promocion p = promocionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Promoción no encontrada con ID: " + id));
        p.setActiva(activa);
        promocionRepository.save(p);
    }
}
