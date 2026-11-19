package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.Rol;

@Getter
@Setter
public class CuentaInternaDTO {

    @NotBlank(message = "El correo de usuario no puede estar vacío.")
    @Email(message = "Formato de correo electrónico inválido.")
    @Size(max = 100, message = "El correo no debe superar los 100 caracteres.")
    private String username;

    @NotBlank(message = "La contraseña no puede estar vacía.")
    @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
    private String password;

    @NotNull(message = "Debe asignar un rol válido.")
    private Rol rol;
}
