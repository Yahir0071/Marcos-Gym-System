package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.Grupo04.dto.SolicitudCongelamientoDTO;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.service.CongelamientoService;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cliente/api/congelamiento")
public class CongelamientoController {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CongelamientoService congelamientoService;

    public CongelamientoController(CongelamientoService congelamientoService) {
        this.congelamientoService = congelamientoService;
    }

    @GetMapping
    public ResponseEntity<?> listarMisSolicitudes(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "mensaje", "Debe iniciar sesión para consultar sus solicitudes."));
        }

        List<Map<String, Object>> solicitudes = congelamientoService.listarPorUsername(authentication.getName())
                .stream()
                .map(this::toMap)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "solicitudes", solicitudes
        ));
    }

    @PostMapping
    public ResponseEntity<?> registrarSolicitud(@Valid @RequestBody SolicitudCongelamientoDTO dto,
                                                BindingResult result,
                                                Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "mensaje", "Debe iniciar sesión para solicitar congelamiento."));
        }

        if (result.hasErrors()) {
            String msg = result.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("success", false, "mensaje", msg));
        }

        try {
            SolicitudCongelamiento guardada = congelamientoService.registrarSolicitud(authentication.getName(), dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "mensaje", "Solicitud de congelamiento registrada y guardada en el sistema.",
                    "solicitud", toMap(guardada)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "mensaje", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(SolicitudCongelamiento s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("dias", s.getDiasSolicitados());
        map.put("fechaInicioSolicitada", s.getFechaInicioSolicitada() != null ? s.getFechaInicioSolicitada().format(FECHA_FMT) : "—");
        map.put("motivo", s.getMotivo());
        map.put("estado", s.getEstado() != null ? s.getEstado().name() : "PENDIENTE");
        map.put("planNombre", s.getMembresia() != null && s.getMembresia().getPlan() != null ? s.getMembresia().getPlan().getNombre() : "Membresía");
        map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().format(FECHA_HORA_FMT) : "—");
        return map;
    }
}
