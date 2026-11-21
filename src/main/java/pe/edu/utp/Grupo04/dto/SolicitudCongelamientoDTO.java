package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCongelamientoDTO {

    @NotNull(message = "Debe seleccionar la cantidad de días a congelar")
    @Min(value = 7, message = "El congelamiento mínimo es de 7 días")
    @Max(value = 15, message = "El congelamiento máximo permitido es de 15 días")
    private Integer dias;

    @NotNull(message = "Debe indicar la fecha de inicio deseada")
    private LocalDate fechaInicioSolicitada;

    @NotBlank(message = "Debe indicar el motivo del congelamiento")
    @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
    private String motivo;
}
