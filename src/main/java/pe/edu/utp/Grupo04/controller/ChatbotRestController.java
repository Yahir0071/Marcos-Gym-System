package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.Grupo04.dto.ChatbotRequestDTO;
import pe.edu.utp.Grupo04.dto.ChatbotResponseDTO;
import pe.edu.utp.Grupo04.exception.AccesoDenegadoException;
import pe.edu.utp.Grupo04.service.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotRestController {

    private final ChatbotService chatbotService;

    public ChatbotRestController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/bienvenida")
    public ResponseEntity<ChatbotResponseDTO> bienvenida(Authentication authentication) {
        verificarNoEsGestorTecnico(authentication);
        return ResponseEntity.ok(chatbotService.obtenerBienvenida(authentication));
    }

    @PostMapping("/mensaje")
    public ResponseEntity<ChatbotResponseDTO> enviarMensaje(@Valid @RequestBody ChatbotRequestDTO request,
                                                            Authentication authentication) {
        verificarNoEsGestorTecnico(authentication);
        return ResponseEntity.ok(chatbotService.responder(request.getMensaje(), authentication));
    }

    private void verificarNoEsGestorTecnico(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_GESTOR_TECNICO".equals(a.getAuthority()))) {
            throw new AccesoDenegadoException("El Gestor Técnico no tiene acceso al asistente comercial de clientes.");
        }
    }
}
