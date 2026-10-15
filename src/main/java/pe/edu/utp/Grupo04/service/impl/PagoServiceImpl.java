package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Pago;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.service.PagoService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

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
    public Pago registrarPagoPresencial(Long membresiaId, Double monto, String metodoPago) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new IllegalArgumentException("Membresía no encontrada con ID: " + membresiaId));

        Pago pago = new Pago();
        pago.setMembresia(membresia);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodoPago(metodoPago);

        // Si la membresía estaba pendiente, se activa
        if (membresia.getEstado() == EstadoMembresia.PENDIENTE) {
            membresia.setEstado(EstadoMembresia.ACTIVA);
            membresiaRepository.save(membresia);
        }

        return pagoRepository.save(pago);
    }
}
