package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipoDTO {

    private Long id;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    private String nombre;

    @NotBlank(message = "El código identificador del equipo es obligatorio")
    private String codigo;

    @NotNull(message = "Debe asignar un estado inicial")
    private EstadoEquipo estado = EstadoEquipo.OPERATIVO;
}
