package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.ReservaDTO;
import pe.edu.utp.Grupo04.exception.AccesoDenegadoException;
import pe.edu.utp.Grupo04.model.Reserva;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.Map;

@Controller
public class ReservaController {

    private final ReservaService reservaService;
    private final SocioService socioService;

    public ReservaController(ReservaService reservaService, SocioService socioService) {
        this.reservaService = reservaService;
        this.socioService = socioService;
    }

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

    // --- C03 / M12: CANCELACIÓN CON VERIFICACIÓN DE PROPIEDAD Y ESTADO ---
    @PostMapping("/cliente/reservas/cancelar")
    public String cancelarReservaCliente(@RequestParam Long reservaId,
                                         Authentication authentication,
                                         RedirectAttributes flash) {
        if (authentication == null || !authentication.isAuthenticated()) {
            flash.addFlashAttribute("mensajeError", "Sesión de usuario no válida.");
            return "redirect:/login";
        }

        try {
            reservaService.cancelarReservaPorCliente(reservaId, authentication.getName());
            flash.addFlashAttribute("mensajeExito", "Reserva cancelada correctamente.");
        } catch (AccesoDenegadoException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/cliente/mis-reservas";
    }

    @PostMapping("/api/reservas")
    @ResponseBody
    public ResponseEntity<?> crearReservaApi(@RequestBody Map<String, Object> payload,
                                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        try {
            Long productoId = Long.valueOf(payload.get("productoId").toString());
            int cantidad = payload.containsKey("cantidad") ? Integer.parseInt(payload.get("cantidad").toString()) : 1;
            Reserva reserva = reservaService.crearReserva(authentication.getName(), productoId, cantidad);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", "#RES-" + String.format("%05d", reserva.getId()),
                    "producto", reserva.getProducto().getNombre(),
                    "cantidad", reserva.getCantidad(),
                    "estado", reserva.getEstado().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/reservas")
    public String reservasAdmin(Model model) {
        model.addAttribute("reservas", reservaService.listarTodas());
        return "admin/reservas";
    }

    @PostMapping("/admin/reservas/cambiar-estado")
    public String cambiarEstadoReservaAdmin(@RequestParam Long id,
                                           @RequestParam EstadoReserva estado,
                                           RedirectAttributes flash) {
        try {
            reservaService.actualizarEstado(id, estado);
            flash.addFlashAttribute("mensajeExito", "Estado de la reserva actualizado a " + estado.name());
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }
}
