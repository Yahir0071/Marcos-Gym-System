package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.ReservaDTO;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

@Controller
public class ReservaController {

    private final ReservaService reservaService;
    private final SocioService socioService;

    public ReservaController(ReservaService reservaService, SocioService socioService) {
        this.reservaService = reservaService;
        this.socioService = socioService;
    }

    // --- ACCIONES DEL CLIENTE ---
    @PostMapping("/cliente/reservar")
    public String reservarProductoCliente(@Valid @ModelAttribute("reservaDTO") ReservaDTO reservaDTO,
                                          BindingResult result,
                                          Authentication authentication,
                                          RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/cliente/productos";
        }

        try {
            reservaService.crearReserva(authentication.getName(), reservaDTO.getProductoId(), reservaDTO.getCantidad());
            flash.addFlashAttribute("mensajeExito", "¡Tu reserva ha sido registrada con éxito! Puedes pasar a pagar y recogerla en recepción.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/cliente/mis-reservas";
    }

    @GetMapping("/cliente/mis-reservas")
    public String misReservasCliente(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("reservas", reservaService.listarPorSocioUsername(username));
        socioService.buscarPorUsername(username).ifPresent(s -> model.addAttribute("socio", s));
        return "cliente/mis-reservas";
    }

    @PostMapping("/cliente/reservas/cancelar")
    public String cancelarReservaCliente(@RequestParam Long reservaId,
                                         Authentication authentication,
                                         RedirectAttributes flash) {
        try {
            reservaService.actualizarEstado(reservaId, EstadoReserva.CANCELADA);
            flash.addFlashAttribute("mensajeExito", "Reserva cancelada correctamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/cliente/mis-reservas";
    }

    @PostMapping("/api/reservas")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> crearReservaApi(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> payload,
                                                                      Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("error", "No autenticado"));
        }
        try {
            Long productoId = Long.valueOf(payload.get("productoId").toString());
            int cantidad = payload.containsKey("cantidad") ? Integer.parseInt(payload.get("cantidad").toString()) : 1;
            pe.edu.utp.Grupo04.model.Reserva reserva = reservaService.crearReserva(authentication.getName(), productoId, cantidad);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "code", "#RES-" + String.format("%05d", reserva.getId()),
                    "producto", reserva.getProducto().getNombre(),
                    "cantidad", reserva.getCantidad(),
                    "estado", reserva.getEstado().name()
            ));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // --- ACCIONES DEL ADMINISTRADOR ---
    @GetMapping("/admin/reservas")
    public String reservasAdmin(Model model) {
        model.addAttribute("reservas", reservaService.listarTodas());
        return "admin/reservas";
    }

    @PostMapping("/admin/reservas/cambiar-estado")
    public String cambiarEstadoReservaAdmin(@RequestParam Long reservaId,
                                           @RequestParam EstadoReserva nuevoEstado,
                                           RedirectAttributes flash) {
        try {
            reservaService.actualizarEstado(reservaId, nuevoEstado);
            flash.addFlashAttribute("mensajeExito", "Estado de la reserva actualizado a: " + nuevoEstado.name());
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }
}
