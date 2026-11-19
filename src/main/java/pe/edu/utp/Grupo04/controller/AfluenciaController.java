package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.AfluenciaDTO;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.service.AfluenciaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Controller
public class AfluenciaController {

    private static final int CAPACIDAD_MAXIMA = 80;

    private final AfluenciaService afluenciaService;

    public AfluenciaController(AfluenciaService afluenciaService) {
        this.afluenciaService = afluenciaService;
    }

    // --- VISTA PÚBLICA DE AFLUENCIA ---
    @GetMapping("/afluencia")
    public String afluenciaPublico(Model model) {
        cargarDatosAfluenciaVista(model);
        return "public/afluencia";
    }

    // --- VISTA DE AFLUENCIA PARA CLIENTE ---
    @GetMapping("/cliente/afluencia")
    public String afluenciaCliente(Model model) {
        cargarDatosAfluenciaVista(model);
        return "cliente/afluencia";
    }

    // --- GESTIÓN DE AFLUENCIA PARA ADMINISTRACIÓN / RECEPCIÓN ---
    @GetMapping("/admin/afluencia")
    public String afluenciaAdmin(Model model) {
        // M13: Una sola consulta ordenada en lugar de 3 llamadas separadas al repositorio
        List<RegistroAfluencia> registros = afluenciaService.listarTodos();
        Map<FranjaHoraria, Integer> vigente = new EnumMap<>(FranjaHoraria.class);
        vigente.put(FranjaHoraria.MANANA, 0);
        vigente.put(FranjaHoraria.MEDIODIA, 0);
        vigente.put(FranjaHoraria.TARDE_NOCHE, 0);

        LocalDateTime ultimaActualizacion = null;
        if (!registros.isEmpty()) {
            LocalDate fechaRef = registros.get(0).getFecha();
            for (RegistroAfluencia r : registros) {
                if (ultimaActualizacion == null || (r.getFechaRegistro() != null && r.getFechaRegistro().isAfter(ultimaActualizacion))) {
                    ultimaActualizacion = r.getFechaRegistro();
                }
                if (fechaRef != null && fechaRef.equals(r.getFecha()) && r.getFranjaHoraria() != null && r.getConcurrenciaAprox() != null) {
                    vigente.put(r.getFranjaHoraria(), Math.max(0, r.getConcurrenciaAprox()));
                }
            }
        }

        model.addAttribute("registros", registros);
        model.addAttribute("afluenciaVigente", vigente);
        model.addAttribute("ultimaActualizacion", ultimaActualizacion);
        return "admin/afluencia";
    }

    @PostMapping("/admin/afluencia/registrar")
    public String registrarAfluenciaAdmin(@Valid @ModelAttribute("afluenciaDTO") AfluenciaDTO afluenciaDTO,
                                         BindingResult result,
                                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/afluencia";
        }

        try {
            afluenciaService.registrarOActualizar(afluenciaDTO.getFecha(),
                    afluenciaDTO.getFranjaHoraria(),
                    afluenciaDTO.getConcurrenciaAprox());
            flash.addFlashAttribute("mensajeExito", "Registro de afluencia guardado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/afluencia";
    }

    private void cargarDatosAfluenciaVista(Model model) {
        Map<FranjaHoraria, Integer> afluencia = afluenciaService.obtenerAfluenciaVigente();
        LocalDateTime ultimaActualizacion = afluenciaService.obtenerUltimaFechaActualizacion();

        int manana = afluencia.getOrDefault(FranjaHoraria.MANANA, 0);
        int mediodia = afluencia.getOrDefault(FranjaHoraria.MEDIODIA, 0);
        int tarde = afluencia.getOrDefault(FranjaHoraria.TARDE_NOCHE, 0);

        int pctManana = calcularPorcentaje(manana);
        int pctMediodia = calcularPorcentaje(mediodia);
        int pctTarde = calcularPorcentaje(tarde);

        boolean hayDatos = ultimaActualizacion != null && (manana > 0 || mediodia > 0 || tarde > 0);
        String mejorFranja;
        if (!hayDatos) {
            mejorFranja = "SIN REGISTROS RECIENTES";
        } else if (pctManana <= pctMediodia && pctManana <= pctTarde) {
            mejorFranja = "06:00 – 11:00 (" + pctManana + "% OCUPACIÓN)";
        } else if (pctMediodia <= pctManana && pctMediodia <= pctTarde) {
            mejorFranja = "11:00 – 16:00 (" + pctMediodia + "% OCUPACIÓN)";
        } else {
            mejorFranja = "16:00 – 23:00 (" + pctTarde + "% OCUPACIÓN)";
        }

        model.addAttribute("afluencia", afluencia);
        model.addAttribute("pctManana", pctManana);
        model.addAttribute("pctMediodia", pctMediodia);
        model.addAttribute("pctTarde", pctTarde);
        model.addAttribute("personasManana", manana);
        model.addAttribute("personasMediodia", mediodia);
        model.addAttribute("personasTarde", tarde);
        model.addAttribute("hayDatosAfluencia", hayDatos);
        model.addAttribute("capacidadMaxima", CAPACIDAD_MAXIMA);
        model.addAttribute("mejorFranjaTexto", mejorFranja);
        model.addAttribute("ultimaActualizacion", ultimaActualizacion);
    }

    private int calcularPorcentaje(int personas) {
        if (personas <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round((personas * 100.0) / CAPACIDAD_MAXIMA));
    }
}
