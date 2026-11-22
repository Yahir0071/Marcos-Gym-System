package pe.edu.utp.Grupo04.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.EstadoNotificacion;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.repository.EstadoNotificacionRepository;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificacionService {
    private final UsuarioRepository usuarios;
    private final EstadoNotificacionRepository estados;
    private final MembresiaService membresias;
    private final ReservaService reservas;
    private final PromocionService promociones;

    public NotificacionService(UsuarioRepository usuarios, EstadoNotificacionRepository estados,
                               MembresiaService membresias, ReservaService reservas, PromocionService promociones) {
        this.usuarios = usuarios;
        this.estados = estados;
        this.membresias = membresias;
        this.reservas = reservas;
        this.promociones = promociones;
    }

    @Transactional(readOnly = true)
    public List<Aviso> listarVisibles(String username) {
        Usuario usuario = usuarios.findByUsername(username).orElseThrow();
        List<Aviso> avisos = generarAvisos(username);
        Map<String, EstadoNotificacion> estadosUsuario = consultarEstados(usuario, avisos);
        return avisos.stream().filter(aviso -> {
            EstadoNotificacion estado = estadosUsuario.get(aviso.clave());
            return estado == null || !estado.isArchivada();
        }).map(aviso -> new Aviso(aviso.clave(), aviso.categoria(), aviso.titulo(), aviso.mensaje(),
                estadosUsuario.containsKey(aviso.clave()) && estadosUsuario.get(aviso.clave()).isLeida()))
                .toList();
    }

    @Transactional
    public boolean actualizar(String username, String clave, boolean archivar) {
        // Serializa escrituras por cuenta y mantiene las acciones repetidas idempotentes.
        Usuario usuario = usuarios.findForUpdateByUsername(username).orElseThrow();
        List<Aviso> avisos = generarAvisos(username).stream()
                .filter(aviso -> clave == null || aviso.clave().equals(clave))
                .toList();
        Map<String, EstadoNotificacion> estadosUsuario = consultarEstados(usuario, avisos);
        List<EstadoNotificacion> cambios = new ArrayList<>();
        for (Aviso aviso : avisos) {
            EstadoNotificacion estado = estadosUsuario.get(aviso.clave());
            if (estado == null) {
                estado = new EstadoNotificacion();
                estado.setUsuarioId(usuario.getId());
                estado.setAvisoClave(aviso.clave());
            }
            if (!estado.isLeida() || (archivar && !estado.isArchivada())) {
                estado.setLeida(true);
                if (archivar) {
                    estado.setArchivada(true);
                }
                cambios.add(estado);
            }
        }
        estados.saveAll(cambios);
        return clave == null || !avisos.isEmpty();
    }

    private Map<String, EstadoNotificacion> consultarEstados(Usuario usuario, List<Aviso> avisos) {
        if (avisos.isEmpty()) {
            return Map.of();
        }
        return estados.findByUsuarioIdAndAvisoClaveIn(usuario.getId(), avisos.stream().map(Aviso::clave).toList())
                .stream().collect(Collectors.toMap(EstadoNotificacion::getAvisoClave, Function.identity()));
    }

    private List<Aviso> generarAvisos(String username) {
        List<Aviso> avisos = new ArrayList<>();
        membresias.obtenerMembresiaActivaPorUsername(username).ifPresent(membresia -> {
            long dias = membresias.calcularDiasRestantes(membresia);
            if (dias <= 7) {
                String vigencia = dias <= 0 ? "Tu membresía vence hoy."
                        : "Te " + (dias == 1 ? "queda 1 día" : "quedan " + dias + " días") + " de membresía.";
                avisos.add(new Aviso("membresia-" + membresia.getId() + "-" + membresia.getFechaFin(),
                        "MEMBRESIA", "Membresía próxima a vencer", vigencia, false));
            }
        });
        reservas.listarPorSocioUsername(username).forEach(reserva -> {
            String estado = switch (reserva.getEstado()) {
                case PENDIENTE -> "Pendiente";
                case CONFIRMADA -> "Confirmada";
                case ENTREGADA -> "Entregada";
                case CANCELADA -> "Cancelada";
            };
            avisos.add(new Aviso("reserva-" + reserva.getId() + "-" + reserva.getEstado(), "RESERVAS",
                    "Reserva de " + reserva.getProducto().getNombre(),
                    "Estado actual: " + estado + " · Cantidad: " + reserva.getCantidad(), false));
        });
        promociones.listarVigentes().forEach(promocion -> avisos.add(new Aviso("promocion-" + promocion.getId(),
                "PROMOCIONES", promocion.getTitulo(), promocion.getDescripcion() == null
                ? "Aprovecha los descuentos vigentes." : promocion.getDescripcion(), false)));
        return avisos;
    }

    public record Aviso(String clave, String categoria, String titulo, String mensaje, boolean leida) {}
}
