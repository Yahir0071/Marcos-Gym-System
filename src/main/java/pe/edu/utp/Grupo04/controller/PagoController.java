package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.PagoDTO;
import pe.edu.utp.Grupo04.exception.ValidacionNegocioException;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.service.PagoService;

import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión administrativa de caja y pagos.
 * Atiende pagos presenciales de membresías (RF11) y auditoría de transacciones y boletas en línea (RF22).
 */
@Controller
public class PagoController {

    private final PagoService pagoService;
    private final OrdenCompraService ordenCompraService;

    public PagoController(PagoService pagoService, OrdenCompraService ordenCompraService) {
        this.pagoService = pagoService;
        this.ordenCompraService = ordenCompraService;
    }

    @GetMapping("/admin/pagos")
    public String pagosAdmin(@RequestParam(defaultValue = "0") int caja,
                             @RequestParam(defaultValue = "0") int linea,
                             @RequestParam(defaultValue = "") String buscar,
                             @RequestParam(defaultValue = "") String pendiente,
                             @RequestParam(defaultValue = "CAJA") String tab,
                             Model model) {
        String filtro = buscar.strip();
        String filtroPendiente = pendiente.strip();
        var pagos = pagoService.listarEnCaja(filtro,
                PageRequest.of(paginaValida(caja), 25, Sort.by(Sort.Direction.DESC, "fechaPago", "id")));
        var compras = ordenCompraService.listarPagina(
                PageRequest.of(paginaValida(linea), 25, Sort.by(Sort.Direction.DESC, "fecha", "id")));
        model.addAttribute("pagos", pagos.getContent());
        model.addAttribute("paginaCaja", pagos);
        model.addAttribute("membresias", pagoService.listarPreinscripcionesPorCobrar(filtroPendiente));
        model.addAttribute("comprasOnline", compras.getContent());
        model.addAttribute("paginaLinea", compras);
        model.addAttribute("buscar", filtro);
        model.addAttribute("pendiente", filtroPendiente);
        model.addAttribute("tabLinea", "LINEA".equals(tab));
        return "admin/pagos";
    }

    @PostMapping("/admin/pagos/registrar")
    public String registrarPagoAdmin(@Valid @ModelAttribute("pagoDTO") PagoDTO pagoDTO,
                                     BindingResult result,
                                     RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/pagos";
        }

        try {
            pagoService.registrarPagoPresencial(pagoDTO.getMembresiaId(), pagoDTO.getMonto(), pagoDTO.getMetodoPago(), pagoDTO.getReferencia());
            flash.addFlashAttribute("mensajeExito", "Pago confirmado. La membresía está activa.");
        } catch (ValidacionNegocioException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            flash.addFlashAttribute("mensajeError", "La referencia ya está registrada. Verifica el historial antes de cobrar.");
        }
        return "redirect:/admin/pagos";
    }

    // --- ENDPOINTS ADMINISTRATIVOS PARA GESTIÓN DE COMPRAS EN LÍNEA Y BOLETAS (RF14 / RF22) ---

    @GetMapping("/admin/api/compras")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarComprasOnlineAdmin(
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        var compras = ordenCompraService.listarPaginaPorCliente(filtro,
                PageRequest.of(paginaValida(page), Math.clamp(size, 1, 100)));
        return ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(compras.getTotalElements()))
                .body(compras.getContent());
    }

    private int paginaValida(int pagina) {
        return Math.clamp(pagina, 0, 100_000);
    }

    @GetMapping("/admin/boleta-venta/descargar")
    public ResponseEntity<byte[]> descargarBoletaInterna(@RequestParam String codigo) {
        return ordenCompraService.buscarPorCodigo(codigo)
                .filter(orden -> "PAGADO".equals(orden.get("estadoPago")))
                .map(CompraApiResponder::boleta)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/api/compras/{codigo}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetalleCompraAdmin(@PathVariable String codigo) {
        return ordenCompraService.buscarPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/api/compras/{codigo}/entregar")
    @ResponseBody
    public ResponseEntity<?> marcarPedidoEntregado(@PathVariable String codigo) {
        boolean entregado = ordenCompraService.marcarEntregado(codigo);
        return ResponseEntity.ok(Map.of("success", entregado, "codigo", codigo));
    }
}
