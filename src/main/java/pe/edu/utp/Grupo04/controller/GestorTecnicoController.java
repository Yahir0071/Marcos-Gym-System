package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.CuentaInternaDTO;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.service.GestionCuentasService;

@Controller
@RequestMapping("/tecnico")
@PreAuthorize("hasRole('GESTOR_TECNICO')")
public class GestorTecnicoController {

    private final GestionCuentasService gestionCuentasService;

    public GestorTecnicoController(GestionCuentasService gestionCuentasService) {
        this.gestionCuentasService = gestionCuentasService;
    }

    @GetMapping({"", "/cuentas"})
    public String listarCuentas(Model model) {
        model.addAttribute("cuentas", gestionCuentasService.listarCuentasInternas());
        model.addAttribute("auditorias", gestionCuentasService.listarAuditoria());
        if (!model.containsAttribute("cuentaDTO")) {
            model.addAttribute("cuentaDTO", new CuentaInternaDTO());
        }
        model.addAttribute("rolesDisponibles", new Rol[]{Rol.RECEPCION, Rol.ADMINISTRACION, Rol.GESTOR_TECNICO});
        return "tecnico/cuentas";
    }

    @PostMapping("/cuentas/crear")
    public String crearCuenta(@Valid @ModelAttribute("cuentaDTO") CuentaInternaDTO cuentaDTO,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cuentaDTO", bindingResult);
            redirectAttributes.addFlashAttribute("cuentaDTO", cuentaDTO);
            redirectAttributes.addFlashAttribute("error", "Datos de cuenta inválidos. Verifique los campos.");
            return "redirect:/tecnico/cuentas";
        }

        try {
            gestionCuentasService.crearCuentaInterna(cuentaDTO, authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Cuenta interna creada exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tecnico/cuentas";
    }

    @PostMapping("/cuentas/{id}/cambiar-rol")
    public String cambiarRol(@PathVariable Long id,
                             @RequestParam("rol") Rol rol,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            gestionCuentasService.cambiarRol(id, rol, authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Rol actualizado exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tecnico/cuentas";
    }

    @PostMapping("/cuentas/{id}/alternar-estado")
    public String alternarEstado(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            gestionCuentasService.alternarEstadoCuenta(id, authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Estado de la cuenta modificado exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tecnico/cuentas";
    }
}
