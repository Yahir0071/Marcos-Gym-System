package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        return promocionRepository.findById(id);
    }

    @Override
    public Promocion guardar(Promocion promocion) {
        return promocionRepository.save(promocion);
    }

    @Override
    public void cambiarEstado(Long id, boolean activa) {
        promocionRepository.findById(id).ifPresent(p -> {
            p.setActiva(activa);
            promocionRepository.save(p);
        });
    }
}
