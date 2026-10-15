package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembresiaAsignacionDTO {

    @NotNull(message = "Debe seleccionar un socio")
    private Long socioId;

    @NotNull(message = "Debe seleccionar un plan de membresía")
    private Long planId;

    private LocalDate fechaInicio;
}
