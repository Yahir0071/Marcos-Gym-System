package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.SocioActualizarDTO;
import pe.edu.utp.Grupo04.dto.SocioRegistroDTO;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.Map;

@Controller
public class SocioController {

    private final SocioService socioService;
    private final PlanService planService;

    public SocioController(SocioService socioService, PlanService planService) {
        this.socioService = socioService;
        this.planService = planService;
    }

    @GetMapping("/admin/socios")
    public String sociosAdmin(Model model) {
        model.addAttribute("socios", socioService.listarTodos());
        model.addAttribute("planes", planService.listarActivos());
        return "admin/socios";
    }

    @PostMapping("/admin/socios/guardar")
    public String guardarSocioAdmin(@Valid @ModelAttribute("socioRegistroDTO") SocioRegistroDTO dto,
                                    BindingResult result,
                                    RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/socios";
        }

        try {
            Socio socio = new Socio();
            socio.setDni(dto.getDni().trim());
            socio.setNombres(dto.getNombres().trim());
            socio.setApellidos(dto.getApellidos().trim());
            socio.setTelefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null);

            socioService.registrarSocio(socio, dto.getEmail().trim(), dto.getPassword());
            flash.addFlashAttribute("mensajeExito", "Socio y usuario registrados exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/socios";
    }

    // --- C08: EDICIÓN PERSISTENTE DE SOCIOS ---
    @PostMapping("/admin/socios/editar/{id}")
    public String editarSocioAdmin(@PathVariable Long id,
                                   @Valid @ModelAttribute("socioEditDTO") SocioActualizarDTO dto,
                                   BindingResult result,
                                   RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/socios";
        }

        try {
            socioService.actualizarSocio(id, dto);
            flash.addFlashAttribute("mensajeExito", "Socio actualizado exitosamente en el sistema.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/socios";
    }

    // --- C08: DESACTIVACIÓN/ACTIVACIÓN PERSISTENTE DE SOCIOS ---
    @PostMapping("/admin/socios/cambiar-estado")
    public String cambiarEstadoSocioAdmin(@RequestParam Long id,
                                         @RequestParam boolean activo,
                                         RedirectAttributes flash) {
        try {
            socioService.cambiarEstado(id, activo);
            flash.addFlashAttribute("mensajeExito", activo ? "Socio activado exitosamente." : "Socio desactivado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/socios";
    }

    // --- API REST JSON: CONTRATO PARA INTERFAZ DINÁMICA (JOSEPH) ---
    @PutMapping("/api/admin/socios/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarSocioApi(@PathVariable Long id,
                                                @Valid @RequestBody SocioActualizarDTO dto) {
        Socio socio = socioService.actualizarSocio(id, dto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Socio actualizado exitosamente en base de datos.",
                "socio", Map.of(
                        "id", socio.getId(),
                        "nombres", socio.getNombres(),
                        "apellidos", socio.getApellidos(),
                        "telefono", socio.getTelefono() != null ? socio.getTelefono() : "",
                        "activo", socio.getUsuario() != null ? socio.getUsuario().getActivo() : true
                )
        ));
    }

    @PatchMapping("/api/admin/socios/{id}/estado")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoSocioApi(@PathVariable Long id,
                                                   @RequestParam boolean activo) {
        socioService.cambiarEstado(id, activo);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", activo ? "Socio activado correctamente" : "Socio desactivado correctamente",
                "activo", activo
        ));
    }
}
