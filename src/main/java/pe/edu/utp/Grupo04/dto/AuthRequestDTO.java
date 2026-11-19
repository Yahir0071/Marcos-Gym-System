package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {

    @NotBlank(message = "El correo de usuario es obligatorio.")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;
}
