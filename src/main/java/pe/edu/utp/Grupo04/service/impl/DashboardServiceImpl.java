package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.service.DashboardService;
import pe.edu.utp.Grupo04.service.EquipoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final SocioService socioService;
    private final MembresiaService membresiaService;
    private final ReservaService reservaService;
    private final EquipoService equipoService;

    public DashboardServiceImpl(SocioService socioService,
                                MembresiaService membresiaService,
                                ReservaService reservaService,
                                EquipoService equipoService) {
        this.socioService = socioService;
        this.membresiaService = membresiaService;
        this.reservaService = reservaService;
        this.equipoService = equipoService;
    }

    @Override
    public Map<String, Object> obtenerMetricasDashboard() {
        Map<String, Object> metricas = new HashMap<>();

        metricas.put("totalSocios", socioService.contarTotalSocios());
        metricas.put("membresiasActivas", membresiaService.contarMembresiasActivas());
        metricas.put("membresiasPorVencer", membresiaService.listarProximasAVencer(7).size());
        metricas.put("reservasPendientes", reservaService.contarPendientes());
        metricas.put("equiposOperativos", equipoService.contarPorEstado(EstadoEquipo.OPERATIVO));
        metricas.put("equiposEnMantenimiento", equipoService.contarPorEstado(EstadoEquipo.EN_MANTENIMIENTO));

        return metricas;
    }
}
