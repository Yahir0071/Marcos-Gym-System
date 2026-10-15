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
