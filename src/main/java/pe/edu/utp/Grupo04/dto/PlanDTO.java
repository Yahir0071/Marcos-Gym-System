package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {

    private Long id;

    @NotBlank(message = "El nombre del plan es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La duración en días es requerida")
    @Min(value = 1, message = "La duración mínima es de 1 día")
    private Integer duracionDias;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser un valor positivo")
    private Double precio;

    private Boolean activo = true;
}
