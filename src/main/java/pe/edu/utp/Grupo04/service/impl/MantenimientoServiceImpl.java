package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Equipo;
import pe.edu.utp.Grupo04.model.Mantenimiento;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.repository.EquipoRepository;
import pe.edu.utp.Grupo04.repository.MantenimientoRepository;
import pe.edu.utp.Grupo04.service.MantenimientoService;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MantenimientoServiceImpl implements MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final EquipoRepository equipoRepository;

    public MantenimientoServiceImpl(MantenimientoRepository mantenimientoRepository, EquipoRepository equipoRepository) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.equipoRepository = equipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mantenimiento> listarTodos() {
        return mantenimientoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mantenimiento> listarPorEquipoId(Long equipoId) {
        return mantenimientoRepository.findByEquipoId(equipoId);
    }

    @Override
    public Mantenimiento registrarMantenimiento(Long equipoId, Mantenimiento mantenimiento) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado con ID: " + equipoId));

        if (mantenimiento.getFechaMantenimiento() == null) {
            mantenimiento.setFechaMantenimiento(LocalDate.now());
        }

        mantenimiento.setEquipo(equipo);

        // Si es correctivo, marcar equipo en mantenimiento
        if ("Correctivo".equalsIgnoreCase(mantenimiento.getTipo())) {
            equipo.setEstado(EstadoEquipo.EN_MANTENIMIENTO);
            equipoRepository.save(equipo);
        }

        return mantenimientoRepository.save(mantenimiento);
    }
}
