package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDTO {

    @NotBlank(message = "Escribe una consulta para continuar.")
    @Size(max = 400, message = "La consulta no puede superar los 400 caracteres.")
    private String mensaje;
}
