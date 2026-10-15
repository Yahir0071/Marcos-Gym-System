package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Pago;

import java.util.List;

public interface PagoService {
    List<Pago> listarTodos();
    List<Pago> listarPorMembresiaId(Long membresiaId);
    Pago registrarPagoPresencial(Long membresiaId, Double monto, String metodoPago);
}
