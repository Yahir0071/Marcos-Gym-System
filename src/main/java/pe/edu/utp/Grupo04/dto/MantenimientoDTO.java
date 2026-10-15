package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotBlank;
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
public class MantenimientoDTO {

    @NotNull(message = "Debe seleccionar un equipo para el mantenimiento")
    private Long equipoId;

    private LocalDate fechaMantenimiento = LocalDate.now();

    @NotBlank(message = "El tipo de mantenimiento (Preventivo/Correctivo) es requerido")
    private String tipo;

    @NotBlank(message = "La descripción de la intervención es obligatoria")
    private String descripcion;

    private String observaciones;
}
