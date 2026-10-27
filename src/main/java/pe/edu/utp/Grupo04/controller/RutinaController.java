package pe.edu.utp.Grupo04.controller;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.Grupo04.service.RutinaService;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/cliente/api/rutina")
public class RutinaController {
    private final RutinaService service;
    public RutinaController(RutinaService service) { this.service = service; }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<?> error(org.springframework.web.server.ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode())
                .body(java.util.Map.of("detail", error.getReason() == null ? "Solicitud inválida" : error.getReason()));
    }
    @GetMapping public RutinaService.Resumen obtener(Principal usuario) { return service.resumen(usuario.getName()); }
    @PostMapping public RutinaService.Resumen guardar(Principal usuario, @RequestBody List<RutinaService.DiaInput> dias) {
        return service.guardar(usuario.getName(), dias);
    }
    @PostMapping("/registro") public RutinaService.Resumen registrar(Principal usuario, @RequestBody RutinaService.RegistroInput registro) {
        return service.registrar(usuario.getName(), registro);
    }
}
