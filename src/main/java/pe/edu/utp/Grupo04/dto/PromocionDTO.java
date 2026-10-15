package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    private String titulo;

    private String descripcion;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @Positive(message = "El porcentaje debe ser positivo")
    private Double porcentajeDescuento;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activa = true;
}
