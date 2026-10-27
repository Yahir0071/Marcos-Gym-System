package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
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
import pe.edu.utp.Grupo04.service.MembresiaService;
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
    private final MembresiaService membresiaService;
    private final OrdenCompraService ordenCompraService;

    public PagoController(PagoService pagoService,
                          MembresiaService membresiaService,
                          OrdenCompraService ordenCompraService) {
        this.pagoService = pagoService;
        this.membresiaService = membresiaService;
        this.ordenCompraService = ordenCompraService;
    }

    @GetMapping("/admin/pagos")
    public String pagosAdmin(Model model) {
        model.addAttribute("pagos", pagoService.listarTodos());
        model.addAttribute("membresias", membresiaService.listarTodas());
        model.addAttribute("comprasOnline", ordenCompraService.listarTodas());
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
            pagoService.registrarPagoPresencial(pagoDTO.getMembresiaId(), pagoDTO.getMonto(), pagoDTO.getMetodoPago());
            flash.addFlashAttribute("mensajeExito", "Pago registrado y membresía actualizada.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/pagos";
    }

    // --- ENDPOINTS ADMINISTRATIVOS PARA GESTIÓN DE COMPRAS EN LÍNEA Y BOLETAS (RF14 / RF22) ---

    @GetMapping("/admin/api/compras")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarComprasOnlineAdmin(@RequestParam(required = false) String filtro) {
        if (filtro != null && !filtro.isBlank()) {
            return ResponseEntity.ok(ordenCompraService.listarPorCliente(filtro));
        }
        return ResponseEntity.ok(ordenCompraService.listarTodas());
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
