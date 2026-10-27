package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.dto.ProductoDTO;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.validation.CompraValidator;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ProductoController {

    private final ProductoService productoService;
    private final SocioService socioService;
    private final OrdenCompraService ordenCompraService;
    private final CompraValidator compraValidator;

    public ProductoController(ProductoService productoService,
                              SocioService socioService,
                              OrdenCompraService ordenCompraService,
                              CompraValidator compraValidator) {
        this.productoService = productoService;
        this.socioService = socioService;
        this.ordenCompraService = ordenCompraService;
        this.compraValidator = compraValidator;
    }

    // --- CATÁLOGO PÚBLICO ---
    @GetMapping("/productos")
    public String productosPublico(Model model) {
        model.addAttribute("productos", productoService.listarCatalogoActivo());
        return "public/productos";
    }

    // --- DETALLE DE PRODUCTO PÚBLICO (CON PASARELA Y PRELLENADO DE SOCIO SI ESTÁ AUTENTICADO) ---
    @GetMapping({"/detalle-producto", "/public/detalle-producto"})
    public String detalleProductoPublico(@RequestParam(required = false) Long id, Model model, Authentication auth) {
        if (id != null) {
            productoService.buscarPorId(id).ifPresent(p -> model.addAttribute("producto", p));
        }
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            socioService.buscarPorUsername(auth.getName()).ifPresent(s -> model.addAttribute("socio", s));
        }
        return "public/detalle-producto";
    }

    // --- DETALLE DE PRODUCTO CLIENTE ---
    @GetMapping("/cliente/detalle-producto")
    public String detalleProductoCliente(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            productoService.buscarPorId(id).ifPresent(p -> model.addAttribute("producto", p));
        }
        return "cliente/detalle-producto";
    }

    // --- CATÁLOGO PARA CLIENTE AUTENTICADO ---
    @GetMapping("/cliente/productos")
    public String productosCliente(Model model) {
        model.addAttribute("productos", productoService.listarCatalogoActivo());
        return "cliente/productos";
    }

    // --- API PÚBLICA DE PASARELA DE PAGO EN LÍNEA PARA PRODUCTOS (RF18/RF21) ---
    @PostMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> registrarCompraApiPublica(@Valid @RequestBody CompraRequestDTO request,
                                                       BindingResult bindingResult,
                                                       Authentication auth) {
        compraValidator.validate(request, bindingResult);
        if (bindingResult.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            for (FieldError err : bindingResult.getFieldErrors()) {
                errores.put(err.getField(), err.getDefaultMessage());
            }
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

    // --- DESCARGA PÚBLICA DE BOLETA DE VENTA ELECTRÓNICA ---
    @GetMapping("/api/boleta-venta/descargar")
    public ResponseEntity<byte[]> descargarBoletaVentaPublica(@RequestParam(required = false) String codigo) {
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
        sb.append("Lugar de Recojo    : Recepción Central Energy Gym (Sede Lince)\n");
        sb.append("Aviso Legal        : Comprobante generado para entorno de evaluación académica.\n");
        sb.append("========================================================================\n");

        byte[] contenido = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "boleta_" + orden.get("boleta") + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(contenido);
    }

    // --- INVENTARIO PARA ADMINISTRACIÓN ---
    @GetMapping("/admin/productos")
    public String productosAdmin(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "admin/productos";
    }

    @PostMapping("/admin/productos/guardar")
    public String guardarProductoAdmin(@Valid @ModelAttribute("productoDTO") ProductoDTO productoDTO,
                                       BindingResult result,
                                       RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/productos";
        }

        Producto producto = new Producto();
        if (productoDTO.getId() != null) {
            producto.setId(productoDTO.getId());
        }
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecio(productoDTO.getPrecio());
        producto.setStock(productoDTO.getStock());
        producto.setImagenUrl(productoDTO.getImagenUrl());
        producto.setActivo(productoDTO.getActivo() != null ? productoDTO.getActivo() : true);

        productoService.guardar(producto);
        flash.addFlashAttribute("mensajeExito", "Producto actualizado en el inventario.");
        return "redirect:/admin/productos";
    }

    @PostMapping("/admin/productos/cambiar-estado")
    public String cambiarEstadoProductoAdmin(@RequestParam Long id,
                                             @RequestParam boolean activo,
                                             RedirectAttributes flash) {
        try {
            productoService.cambiarEstado(id, activo);
            flash.addFlashAttribute("mensajeExito", "Estado del producto actualizado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al cambiar estado: " + e.getMessage());
        }
        return "redirect:/admin/productos";
    }
}
