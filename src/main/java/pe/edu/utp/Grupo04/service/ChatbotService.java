package pe.edu.utp.Grupo04.service;

import org.springframework.security.core.Authentication;
import pe.edu.utp.Grupo04.dto.ChatbotResponseDTO;

public interface ChatbotService {

    ChatbotResponseDTO obtenerBienvenida(Authentication authentication);

    ChatbotResponseDTO responder(String mensaje, Authentication authentication);
}
