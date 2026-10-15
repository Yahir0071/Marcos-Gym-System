package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AfluenciaDTO {

    private LocalDate fecha;

    @NotNull(message = "Debe seleccionar una franja horaria")
    private FranjaHoraria franjaHoraria;

    @NotNull(message = "La concurrencia aproximada es obligatoria")
    @Min(value = 0, message = "La concurrencia no puede ser negativa")
    private Integer concurrenciaAprox;
}
