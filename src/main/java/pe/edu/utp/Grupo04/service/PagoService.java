package pe.edu.utp.Grupo04.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Pago;

import java.math.BigDecimal;
import java.util.List;

public interface PagoService {
    List<Pago> listarTodos();
    List<Pago> listarPorMembresiaId(Long membresiaId);
    Page<Pago> listarEnCaja(String filtro, Pageable pageable);
    List<Membresia> listarPreinscripcionesPorCobrar(String filtro);
    Pago registrarPagoPresencial(Long membresiaId, BigDecimal monto, String metodoPago, String referencia);
}
