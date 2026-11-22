package pe.edu.utp.Grupo04.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.exception.ValidacionNegocioException;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Pago;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.service.PagoService;
import pe.edu.utp.Grupo04.service.TarifaInscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private static final Map<String, String> METODOS = Map.of(
            "EFECTIVO", "Efectivo", "TARJETA", "Tarjeta", "TRANSFERENCIA", "Transferencia", "BILLETERA", "Billetera digital");

    private final PagoRepository pagoRepository;
    private final MembresiaRepository membresiaRepository;

    public PagoServiceImpl(PagoRepository pagoRepository, MembresiaRepository membresiaRepository) {
        this.pagoRepository = pagoRepository;
        this.membresiaRepository = membresiaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> listarPorMembresiaId(Long membresiaId) {
        return pagoRepository.findByMembresiaId(membresiaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Pago> listarEnCaja(String filtro, Pageable pageable) {
        return pagoRepository.listarEnCaja(filtro.strip().toLowerCase(Locale.ROOT), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Membresia> listarPreinscripcionesPorCobrar(String filtro) {
        return membresiaRepository.buscarPreinscripcionesPorCobrar(EstadoMembresia.PENDIENTE,
                LocalDateTime.now(TarifaInscripcion.ZONA_LIMA), filtro.strip().toUpperCase(Locale.ROOT), PageRequest.of(0, 50));
    }

    @Override
    public Pago registrarPagoPresencial(Long membresiaId, BigDecimal monto, String metodoPago, String referencia) {
        String metodo = metodoPago == null ? "" : metodoPago.strip().toUpperCase(Locale.ROOT);
        if (!METODOS.containsKey(metodo)) {
            throw new ValidacionNegocioException("Selecciona un método de pago presencial válido.");
        }
        String clave = referencia == null ? "" : referencia.strip().toUpperCase(Locale.ROOT);
        if (!clave.matches("[A-Z0-9][A-Z0-9 ._/-]{0,79}")) {
            throw new ValidacionNegocioException("Ingresa una referencia válida de hasta 80 caracteres.");
        }
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new ValidacionNegocioException("Ingresa un monto válido con hasta dos decimales.");
        }
        Membresia membresia = membresiaRepository.buscarParaActualizar(membresiaId)
                .orElseThrow(() -> new ValidacionNegocioException("La membresía indicada no existe."));
        Pago anterior = pagoRepository.findByReferencia(clave).orElse(null);
        if (anterior != null) {
            if (membresiaId.equals(anterior.getMembresia().getId())
                    && monto.compareTo(BigDecimal.valueOf(anterior.getMonto())) == 0
                    && METODOS.get(metodo).equals(anterior.getMetodoPago())) {
                return anterior;
            }
            throw new ValidacionNegocioException("La referencia ya corresponde a otro registro de pago.");
        }
        if (membresia.getEstado() != EstadoMembresia.PENDIENTE || pagoRepository.existsByMembresiaId(membresiaId)) {
            throw new ValidacionNegocioException("La membresía ya se pagó o no está disponible para cobro.");
        }

        LocalDateTime ahora = LocalDateTime.now(TarifaInscripcion.ZONA_LIMA);
        if (membresia.getMontoReservado() == null || membresia.getPreinscripcionVence() == null
                || !ahora.isBefore(membresia.getPreinscripcionVence())) {
            throw new ValidacionNegocioException("La preinscripción no está vigente para cobro.");
        }
        if (monto.compareTo(membresia.getMontoReservado()) != 0) {
            throw new ValidacionNegocioException("Registra el monto completo de la preinscripción: S/ "
                    + membresia.getMontoReservado().toPlainString() + ".");
        }
        LocalDate inicio = ahora.toLocalDate();
        LocalDate fin = switch (membresia.getPeriodoReservado()) {
            case "ANUAL" -> inicio.plusYears(1);
            case "TRIMESTRAL" -> inicio.plusMonths(3);
            default -> inicio.plusDays(ChronoUnit.DAYS.between(membresia.getFechaInicio(), membresia.getFechaFin()));
        };
        membresia.setFechaInicio(inicio);
        membresia.setFechaFin(fin);
        membresia.setEstado(EstadoMembresia.ACTIVA);
        membresiaRepository.save(membresia);

        Pago pago = new Pago();
        pago.setMembresia(membresia);
        pago.setMonto(monto.doubleValue());
        pago.setFechaPago(ahora);
        pago.setMetodoPago(METODOS.get(metodo));
        pago.setReferencia(clave);
        return pagoRepository.save(pago);
    }
}
