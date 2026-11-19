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
import org.springframework.web.util.UriUtils;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.nio.charset.StandardCharsets;
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
    private final ProductoService productoService;
    private final OrdenCompraService ordenCompraService;
    private final CompraApiResponder responder;

    public CompraClienteController(SocioService socioService,
                                   ProductoService productoService,
                                   OrdenCompraService ordenCompraService,
                                   CompraApiResponder responder) {
        this.socioService = socioService;
        this.productoService = productoService;
        this.ordenCompraService = ordenCompraService;
        this.responder = responder;
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
    public String pagoEnLinea(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/pago-en-linea";
    }

    @GetMapping("/mis-compras")
    public String misCompras(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/mis-compras";
    }

    @GetMapping("/pago-en-proceso")
    public String pagoEnProceso(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        return vistaPedido("cliente/pago-en-proceso", ordenDelUsuario(codigo, auth), auth, model);
    }

    @GetMapping("/pago-rechazado")
    public String pagoRechazado(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        return vistaPedido("cliente/pago-rechazado", ordenDelUsuario(codigo, auth), auth, model);
    }

    @GetMapping("/pago-cancelado")
    public String pagoCancelado(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        return vistaPedido("cliente/pago-cancelado", ordenDelUsuario(codigo, auth), auth, model);
    }

    @GetMapping("/compra-exitosa")
    public String compraExitosa(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        Map<String, Object> orden = ordenDelUsuario(codigo, auth);
        String estado = String.valueOf(orden.get("estadoPago"));
        if (!"PAGADO".equals(estado)) {
            // "Compra exitosa" solo con un pago aprobado y guardado
            String destino = switch (estado) {
                case "EN_PROCESO" -> "/cliente/pago-en-proceso";
                case "RECHAZADO" -> "/cliente/pago-rechazado";
                default -> "/cliente/pago-cancelado";
            };
            return "redirect:" + destino + "?codigo=" + UriUtils.encode(codigo, StandardCharsets.UTF_8);
        }
        return vistaPedido("cliente/compra-exitosa", orden, auth, model);
    }

    @GetMapping("/detalle-pedido")
    public String detallePedido(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        return vistaPedido("cliente/detalle-pedido", ordenDelUsuario(codigo, auth), auth, model);
    }

    @GetMapping("/boleta-venta")
    public String boletaVenta(@RequestParam(required = false) String codigo,
                              Authentication auth, Model model) {
        return vistaPedido("cliente/boleta-venta", ordenDelUsuario(codigo, auth), auth, model);
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
        model.addAttribute("productosCatalogo", productoService.listarCatalogoActivo());
        model.addAttribute("ordenesMemoria",
                username != null ? ordenCompraService.listarDeSocio(username) : List.of());
        model.addAttribute("proximoCodigoPedido", ordenCompraService.obtenerProximoCodigo());
    }
}