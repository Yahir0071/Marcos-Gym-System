package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.validation.CompraValidator;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador web y API REST para el flujo de compras, pedidos y pasarela de pago en línea del cliente.
 * Diseñado bajo principios SOLID (Single Responsibility, Dependency Inversion), delega reglas a OrdenCompraService
 * y validaciones a CompraValidator conforme a la Figura 5 del informe.
 */
@Slf4j
@Controller
@RequestMapping("/cliente")
public class CompraClienteController {

    private final SocioService socioService;
    private final ProductoService productoService;
    private final OrdenCompraService ordenCompraService;
    private final CompraValidator compraValidator;
    private final pe.edu.utp.Grupo04.service.ReservaService reservaService;

    public CompraClienteController(SocioService socioService,
                                   ProductoService productoService,
                                   OrdenCompraService ordenCompraService,
                                   CompraValidator compraValidator,
                                   pe.edu.utp.Grupo04.service.ReservaService reservaService) {
        this.socioService = socioService;
        this.productoService = productoService;
        this.ordenCompraService = ordenCompraService;
        this.compraValidator = compraValidator;
        this.reservaService = reservaService;
    }

    private void cargarDatosSocio(Authentication auth, Model model) {
        Socio socio = null;
        String username = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            username = auth.getName();
            socio = socioService.buscarPorUsername(username).orElse(null);
        }
        if (socio == null) {
            socio = new Socio();
            socio.setNombres("Carlos");
            socio.setApellidos("Mendoza Rojas");
            socio.setDni("72563123");
            socio.setTelefono("987654321");
        }
        model.addAttribute("socio", socio);
        model.addAttribute("productosCatalogo", productoService.listarCatalogoActivo());
        model.addAttribute("ordenesMemoria", ordenCompraService.listarPorCliente(username != null ? username : socio.getDni()));
        model.addAttribute("proximoCodigoPedido", ordenCompraService.obtenerProximoCodigo());
    }

    // --- API REST PARA CREAR COMPRAS / BOLETAS Y DESCONTAR STOCK CON VALIDACIÓN SPRING ---
    @PostMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> registrarCompraApi(@Valid @RequestBody CompraRequestDTO request,
                                                BindingResult bindingResult,
                                                Authentication auth) {
        // Ejecutar validador personalizado de Spring (Figura 5 de la arquitectura)
        compraValidator.validate(request, bindingResult);

        if (bindingResult.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            for (FieldError err : bindingResult.getFieldErrors()) {
                errores.put(err.getField(), err.getDefaultMessage());
            }
            log.warn("Rechazo de compra por errores de validación: {}", errores);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", "Datos de pago incompletos o inválidos",
                    "errores", errores
            ));
        }

        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : null;

        Map<String, Object> orden = ordenCompraService.registrarCompra(request, username);
        String codigo = (String) orden.get("codigo");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "codigo", codigo,
                "boleta", orden.get("boleta"),
                "orden", orden
        ));
    }

    @PostMapping("/api/compras/aprobar")
    @ResponseBody
    public ResponseEntity<?> aprobarCompraEnProceso(@RequestParam(defaultValue = "#PED-00521") String codigo) {
        boolean aprobada = ordenCompraService.aprobarCompra(codigo);
        return ResponseEntity.ok(Map.of("success", aprobada, "codigo", codigo));
    }

    @GetMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> listarComprasEnMemoria(Authentication auth) {
        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : null;
        return ResponseEntity.ok(ordenCompraService.listarPorCliente(username));
    }

    // --- ENDPOINT DESCARGA BOLETA ELECTRÓNICA (RF21) ---
    @GetMapping("/boleta-venta/descargar")
    public ResponseEntity<byte[]> descargarBoletaVenta(@RequestParam(required = false) String codigo) {
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                       ENERGY GYM - COMPROBANTE DE PAGO                 \n");
        sb.append("                   PROTOTIPO — SIN VALIDEZ TRIBUTARIA                   \n");
        sb.append("========================================================================\n");
        sb.append("Boleta Electrónica : ").append(orden.get("boleta")).append("\n");
        sb.append("Código de Pedido   : ").append(orden.get("codigo")).append("\n");
        sb.append("N° de Operación    : ").append(orden.get("operacion")).append("\n");
        sb.append("Fecha de Emisión   : ").append(orden.get("fecha")).append("\n");
        sb.append("Estado de Pago     : ").append(orden.get("estadoPago")).append("\n");
        sb.append("Estado de Entrega  : ").append(orden.get("estadoEntrega")).append("\n");
        sb.append("------------------------------------------------------------------------\n");
        sb.append("DATOS DEL CLIENTE\n");
        sb.append("Cliente            : ").append(orden.get("buyerName")).append("\n");
        sb.append("Documento          : ").append(orden.get("buyerDocType")).append(" ").append(orden.get("buyerDocNum")).append("\n");
        sb.append("Correo             : ").append(orden.get("buyerEmail")).append("\n");
        sb.append("Método de Pago     : Tarjeta ").append(orden.get("cardBrand")).append(" **** ").append(orden.get("cardLast4")).append("\n");
        sb.append("------------------------------------------------------------------------\n");
        sb.append("DETALLE DE CONCEPTOS ADQUIRIDOS\n");
        sb.append(orden.get("resumenItems")).append("\n");
        sb.append("------------------------------------------------------------------------\n");
        sb.append(String.format("Op. Gravada (Base) : S/ %s\n", orden.get("baseFormatted")));
        sb.append(String.format("I.G.V. (18%%)       : S/ %s\n", orden.get("igvFormatted")));
        sb.append(String.format("IMPORTE TOTAL      : S/ %s\n", orden.get("totalFormatted")));
        sb.append("========================================================================\n");
        sb.append("Lugar de Recojo    : Recepción Central Energy Gym (Av. Principal 123)\n");
        sb.append("Aviso Legal        : Comprobante generado para entorno de evaluación académica.\n");
        sb.append("========================================================================\n");

        byte[] contenido = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "boleta_" + orden.get("boleta") + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(contenido);
    }

    // --- VISTAS CLIENTE (THYMELEAF) ---

    // 1. Tu carrito
    @GetMapping("/carrito")
    public String verCarrito(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/carrito";
    }

    // 2. Pago en línea
    @GetMapping("/pago-en-linea")
    public String pagoEnLinea(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/pago-en-linea";
    }

    // 3. Pago en proceso
    @GetMapping("/pago-en-proceso")
    public String pagoEnProceso(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/pago-en-proceso";
    }

    // 4. Pago rechazado
    @GetMapping("/pago-rechazado")
    public String pagoRechazado(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/pago-rechazado";
    }

    // 5. Pago cancelado
    @GetMapping("/pago-cancelado")
    public String pagoCancelado(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/pago-cancelado";
    }

    // 6. Compra exitosa
    @GetMapping("/compra-exitosa")
    public String compraExitosa(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/compra-exitosa";
    }

    // 7. Detalle del pedido
    @GetMapping("/detalle-pedido")
    public String detallePedido(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/detalle-pedido";
    }

    // 8. Mis compras
    @GetMapping("/mis-compras")
    public String misCompras(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/mis-compras";
    }

    // 9. Boleta de venta
    @GetMapping("/boleta-venta")
    public String boletaVenta(@RequestParam(required = false) String codigo,
                              Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = ordenCompraService.buscarPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/boleta-venta";
    }
}
