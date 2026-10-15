package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.repository.RegistroAfluenciaRepository;
import pe.edu.utp.Grupo04.service.AfluenciaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AfluenciaServiceImpl implements AfluenciaService {

    private final RegistroAfluenciaRepository afluenciaRepository;

    public AfluenciaServiceImpl(RegistroAfluenciaRepository afluenciaRepository) {
        this.afluenciaRepository = afluenciaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAfluencia> listarTodos() {
        return afluenciaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAfluencia> listarPorFecha(LocalDate fecha) {
        return afluenciaRepository.findByFecha(fecha);
    }

    @Override
    public RegistroAfluencia registrarOActualizar(LocalDate fecha, FranjaHoraria franjaHoraria, Integer concurrencia) {
        LocalDate fechaRegistro = (fecha != null) ? fecha : LocalDate.now();

        Optional<RegistroAfluencia> existente = afluenciaRepository.findByFechaAndFranjaHoraria(fechaRegistro, franjaHoraria);

        RegistroAfluencia registro;
        if (existente.isPresent()) {
            registro = existente.get();
            registro.setConcurrenciaAprox(concurrencia);
            registro.setFechaRegistro(LocalDateTime.now());
        } else {
            registro = new RegistroAfluencia();
            registro.setFecha(fechaRegistro);
            registro.setFranjaHoraria(franjaHoraria);
            registro.setConcurrenciaAprox(concurrencia);
            registro.setFechaRegistro(LocalDateTime.now());
        }

        return afluenciaRepository.save(registro);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FranjaHoraria, Integer> obtenerAfluenciaVigente() {
        Map<FranjaHoraria, Integer> mapa = new EnumMap<>(FranjaHoraria.class);
        mapa.put(FranjaHoraria.MANANA, 0);
        mapa.put(FranjaHoraria.MEDIODIA, 0);
        mapa.put(FranjaHoraria.TARDE_NOCHE, 0);

        LocalDate hoy = LocalDate.now();
        List<RegistroAfluencia> registrosHoy = afluenciaRepository.findByFecha(hoy);

        if (!registrosHoy.isEmpty()) {
            for (RegistroAfluencia r : registrosHoy) {
                mapa.put(r.getFranjaHoraria(), r.getConcurrenciaAprox());
            }
        } else {
            // Si hoy aún no hay registros, tomar los más recientes de la base de datos
            List<RegistroAfluencia> todos = afluenciaRepository.findAll();
            for (RegistroAfluencia r : todos) {
                mapa.put(r.getFranjaHoraria(), r.getConcurrenciaAprox());
            }
        }

        return mapa;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime obtenerUltimaFechaActualizacion() {
        return afluenciaRepository.findTopByOrderByFechaRegistroDesc()
                .map(RegistroAfluencia::getFechaRegistro)
                .orElse(LocalDateTime.now());
    }
}
