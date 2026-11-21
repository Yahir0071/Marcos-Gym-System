package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class PromocionDTO {

    private Long id;

    @NotBlank(message = "El título de la promoción es obligatorio")
    @Size(max = 150, message = "El título de la promoción no puede exceder los 150 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String descripcion;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El porcentaje de descuento debe ser mayor a 0%")
    @DecimalMax(value = "100.00", message = "El porcentaje de descuento no puede superar el 100%")
    private Double porcentajeDescuento;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    private Boolean activa = true;
}
