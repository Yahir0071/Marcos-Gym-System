package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {

    @NotNull(message = "Debe asociar el pago a una membresía")
    private Long membresiaId;

    @NotNull(message = "El monto del pago es obligatorio")
    @Positive(message = "El monto debe ser un valor positivo")
    @Digits(integer = 10, fraction = 2, message = "Ingresa un monto con hasta dos decimales")
    private BigDecimal monto;

    @NotBlank(message = "Debe especificar el método de pago presencial")
    private String metodoPago;

    @NotBlank(message = "Ingresa la referencia del cobro")
    @Size(max = 80, message = "La referencia admite hasta 80 caracteres")
    private String referencia;
}
