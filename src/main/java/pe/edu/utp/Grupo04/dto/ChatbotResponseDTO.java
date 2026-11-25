package pe.edu.utp.Grupo04.dto;

import java.util.List;
import java.util.Map;

public record ChatbotResponseDTO(
        boolean success,
        String respuesta,
        String modo,
        boolean iaDisponible,
        Integer consultasRestantesIa,
        List<String> sugerencias,
        List<Map<String, String>> enlaces
) {
}
