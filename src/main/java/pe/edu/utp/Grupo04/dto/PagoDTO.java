package pe.edu.utp.Grupo04.dto;

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
public class PagoDTO {

    @NotNull(message = "Debe asociar el pago a una membresía")
    private Long membresiaId;

    @NotNull(message = "El monto del pago es obligatorio")
    @Positive(message = "El monto debe ser un valor positivo")
    private Double monto;

    @NotBlank(message = "Debe especificar el método de pago (Efectivo, Yape, Plin, Tarjeta)")
    private String metodoPago;
}
