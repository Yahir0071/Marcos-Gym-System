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
        return afluenciaRepository.findAllByOrderByFechaDescFechaRegistroDesc();
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
        List<RegistroAfluencia> registrosVigentes = afluenciaRepository.findByFecha(hoy);

        if (registrosVigentes.isEmpty()) {
            // M13: Consultar únicamente los registros de la fecha más reciente en vez de cargar toda la tabla con findAll()
            Optional<LocalDate> ultimaFecha = afluenciaRepository.findUltimaFechaRegistrada();
            if (ultimaFecha.isPresent()) {
                registrosVigentes = afluenciaRepository.findByFecha(ultimaFecha.get());
            }
        }

        for (RegistroAfluencia r : registrosVigentes) {
            if (r.getFranjaHoraria() != null && r.getConcurrenciaAprox() != null) {
                mapa.put(r.getFranjaHoraria(), Math.max(0, r.getConcurrenciaAprox()));
            }
        }

        return mapa;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime obtenerUltimaFechaActualizacion() {
        // M07: Usar fecha real de la base de datos; si no existen registros devolver null (sin inventar fecha actual)
        return afluenciaRepository.findTopByOrderByFechaRegistroDesc()
                .map(RegistroAfluencia::getFechaRegistro)
                .orElse(null);
    }
}
