package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/tecnico")
@PreAuthorize("hasRole('GESTOR_TECNICO')")
public class GestorTecnicoController {

    private static final Logger LOG = LoggerFactory.getLogger(GestorTecnicoController.class);
    private final GestionCuentasService gestionCuentasService;

    public GestorTecnicoController(GestionCuentasService gestionCuentasService) {
        this.gestionCuentasService = gestionCuentasService;
    }

    @GetMapping({"", "/cuentas"})
    public String listarCuentas(@RequestParam(defaultValue = "0") int pagina, Model model) {
        cargarPanel(model, pagina);
        return "tecnico/cuentas";
    }

    private void cargarPanel(Model model, int pagina) {
        var cuentas = gestionCuentasService.listarCuentasInternas(Math.max(pagina, 0));
        model.addAttribute("cuentas", cuentas.getContent());
        model.addAttribute("paginaCuentas", cuentas);
        model.addAttribute("auditorias", gestionCuentasService.listarAuditoria());
        if (!model.containsAttribute("cuentaDTO")) {
            model.addAttribute("cuentaDTO", new CuentaInternaDTO());
        }
        model.addAttribute("rolesDisponibles", new Rol[]{Rol.RECEPCION, Rol.ADMINISTRACION, Rol.GESTOR_TECNICO});
    }

    @PostMapping("/cuentas/crear")
    public String crearCuenta(@Valid @ModelAttribute("cuentaDTO") CuentaInternaDTO cuentaDTO,
                              BindingResult bindingResult,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            cuentaDTO.setPassword(null);
            model.addAttribute("error", "Revisa los campos de la cuenta interna.");
            cargarPanel(model, 0);
            return "tecnico/cuentas";
        }

        try {
            gestionCuentasService.crearCuentaInterna(cuentaDTO, authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Cuenta interna creada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo crear la cuenta. Verifica si el correo ya está registrado.");
        } catch (RuntimeException e) {
            informarError(redirectAttributes, e);
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
            redirectAttributes.addFlashAttribute("exito", "Rol guardado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            informarError(redirectAttributes, e);
        }
        return "redirect:/tecnico/cuentas";
    }

    @PostMapping("/cuentas/{id}/alternar-estado")
    public String alternarEstado(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            gestionCuentasService.alternarEstadoCuenta(id, authentication.getName());
            redirectAttributes.addFlashAttribute("exito", "Estado de la cuenta actualizado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            informarError(redirectAttributes, e);
        }
        return "redirect:/tecnico/cuentas";
    }

    private void informarError(RedirectAttributes redirectAttributes, RuntimeException error) {
        LOG.error("No se pudo guardar el cambio de cuenta interna", error);
        redirectAttributes.addFlashAttribute("error", "No se pudo guardar el cambio. Inténtalo de nuevo.");
    }
}
