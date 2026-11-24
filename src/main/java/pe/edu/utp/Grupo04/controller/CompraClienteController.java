package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.model.enums.PeriodoMembresia;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.List;
import java.util.Map;

/**
 * Compras simuladas del cliente: carrito, pago ficticio, pedidos y comprobantes.
 * Todo pedido se consulta por código Y propietario (sesión).
 */
@Controller
@RequestMapping("/cliente")
public class CompraClienteController {

    private final SocioService socioService;
    private final OrdenCompraService ordenCompraService;
    private final CompraApiResponder responder;
    private final ReservaService reservaService;
    private final PlanService planService;

    public CompraClienteController(SocioService socioService,
                                   OrdenCompraService ordenCompraService,
                                   CompraApiResponder responder, ReservaService reservaService,
                                   PlanService planService) {
        this.socioService = socioService;
        this.ordenCompraService = ordenCompraService;
        this.responder = responder;
        this.reservaService = reservaService;
        this.planService = planService;
    }

    // ------------------------------------------------------------------ API
    @PostMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> registrarCompraApi(@Valid @RequestBody CompraRequestDTO request,
                                                BindingResult bindingResult,
                                                Authentication auth) {
        return responder.procesarCompra(request, bindingResult, auth);
    }

    @PostMapping("/api/compras/aprobar")
    @ResponseBody
    public ResponseEntity<?> aprobar(@RequestParam String codigo, Authentication auth) {
        return responder.resolverPendiente(true, codigo, auth);
    }

    @PostMapping("/api/compras/cancelar")
    @ResponseBody
    public ResponseEntity<?> cancelar(@RequestParam String codigo, Authentication auth) {
        return responder.resolverPendiente(false, codigo, auth);
    }

    @GetMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> listarMisCompras(Authentication auth) {
        String username = CompraApiResponder.username(auth);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ordenCompraService.listarDeSocio(username));
    }

    @GetMapping("/boleta-venta/descargar")
    public ResponseEntity<byte[]> descargarBoletaVenta(@RequestParam(required = false) String codigo,
                                                       Authentication auth) {
        return responder.descargarBoleta(codigo, auth);
    }

    // ------------------------------------------------------------------ VISTAS
    @GetMapping("/carrito")
    public String verCarrito(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/carrito";
    }

    @GetMapping("/pago-en-linea")
    public String pagoEnLinea(@RequestParam(required = false) Long reservaId,
                              @RequestParam(required = false) Long planId,
                              @RequestParam(required = false) String periodo,
                              Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        if (planId != null && reservaId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una sola operación.");
        }
        if (planId != null) {
            var plan = planService.buscarPorId(planId)
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no disponible"));
            PeriodoMembresia seleccionado = PeriodoMembresia.interpretar(periodo);
            model.addAttribute("planCheckout", plan);
            model.addAttribute("periodoCheckout", seleccionado);
            model.addAttribute("totalPlanCheckout", seleccionado.total(plan.getPrecio()));
        }
        if (reservaId != null) {
            var reserva = reservaService.buscarPorId(reservaId)
                    .filter(r -> r.getSocio().getUsuario() != null
                            && r.getSocio().getUsuario().getUsername().equals(auth.getName()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
            if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
                return "redirect:/cliente/mis-reservas";
            }
            model.addAttribute("reservaCheckout", reserva);
        }
        return "cliente/pago-en-linea";
    }

    @GetMapping("/mis-compras")
    public String misCompras(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        String username = CompraApiResponder.username(auth);
        model.addAttribute("ordenesMemoria", username != null ? ordenCompraService.listarDeSocio(username) : List.of());
        return "cliente/mis-compras";
    }

    @GetMapping({"/compra-exitosa", "/pago-en-proceso", "/pago-rechazado", "/pago-cancelado"})
    public String resultadoPago(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        return vistaPedido("cliente/resultado-pago", ordenDelUsuario(codigo, auth), auth, model);
    }

    @GetMapping("/detalle-pedido")
    public String detallePedido(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        return vistaPedido("cliente/detalle-pedido", ordenDelUsuario(codigo, auth), auth, model);
    }

    @GetMapping("/boleta-venta")
    public String boletaVenta(@RequestParam(required = false) String codigo,
                              Authentication auth, Model model) {
        Map<String, Object> orden = ordenDelUsuario(codigo, auth);
        if (!"PAGADO".equals(orden.get("estadoPago"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no disponible");
        }
        return vistaPedido("cliente/boleta-venta", orden, auth, model);
    }

    // ------------------------------------------------------------------ AUXILIARES
    /** Pedido del usuario en sesión; si no existe o es ajeno => 404. */
    private Map<String, Object> ordenDelUsuario(String codigo, Authentication auth) {
        return ordenCompraService
                .buscarPorCodigoYPropietario(codigo, CompraApiResponder.username(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    private String vistaPedido(String plantilla, Map<String, Object> orden,
                               Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return plantilla;
    }

    private void cargarDatosSocio(Authentication auth, Model model) {
        String username = CompraApiResponder.username(auth);
        Socio socio = (username != null) ? socioService.buscarPorUsername(username).orElse(null) : null;
        model.addAttribute("socio", socio != null ? socio : new Socio());   // sin datos ficticios
    }
}
