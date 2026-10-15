package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Equipo;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.repository.EquipoRepository;
import pe.edu.utp.Grupo04.service.EquipoService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EquipoServiceImpl implements EquipoService {

    private final EquipoRepository equipoRepository;

    public EquipoServiceImpl(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipo> listarTodos() {
        return equipoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipo> listarPorEstado(EstadoEquipo estado) {
        return equipoRepository.findByEstado(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Equipo> buscarPorId(Long id) {
        return equipoRepository.findById(id);
    }

    @Override
    public Equipo guardar(Equipo equipo) {
        return equipoRepository.save(equipo);
    }

    @Override
    public Equipo cambiarEstado(Long id, EstadoEquipo nuevoEstado) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado con ID: " + id));

        equipo.setEstado(nuevoEstado);
        return equipoRepository.save(equipo);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarEquipos() {
        return equipoRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPorEstado(EstadoEquipo estado) {
        return equipoRepository.findByEstado(estado).size();
    }
}
