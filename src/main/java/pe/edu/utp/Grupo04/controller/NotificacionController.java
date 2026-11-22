package pe.edu.utp.Grupo04.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.Grupo04.service.NotificacionService;

@Controller
public class NotificacionController {
    private final NotificacionService notificaciones;

    public NotificacionController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/cliente/notificaciones")
    public String listar(Authentication autenticacion, Model model) {
        String username = autenticacion.getName();
        model.addAttribute("notificaciones", notificaciones.listarVisibles(username));
        return "cliente/notificaciones";
    }

    @PostMapping("/cliente/api/notificaciones/leer")
    @ResponseBody
    public ResponseEntity<Void> leer(Authentication autenticacion, @RequestParam(required = false) String clave) {
        return actualizar(autenticacion, clave, false);
    }

    @PostMapping("/cliente/api/notificaciones/archivar")
    @ResponseBody
    public ResponseEntity<Void> archivar(Authentication autenticacion, @RequestParam String clave) {
        return actualizar(autenticacion, clave, true);
    }

    private ResponseEntity<Void> actualizar(Authentication autenticacion, String clave, boolean archivar) {
        if (clave != null && (clave.isBlank() || clave.length() > 80)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del aviso no es válido.");
        }
        if (!notificaciones.actualizar(autenticacion.getName(), clave, archivar)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El aviso no está disponible.");
        }
        return ResponseEntity.noContent().build();
    }
}
