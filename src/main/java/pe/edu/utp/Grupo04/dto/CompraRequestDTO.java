package pe.edu.utp.Grupo04.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequestDTO {

    private String tipoConcepto = "PRODUCTO"; // "PRODUCTO" o "MEMBRESIA"

    private Long planId; // Obligatorio si tipoConcepto == "MEMBRESIA"

    @Size(max = 15, message = "Periodo de membresía no válido")
    private String periodo;

    private Long reservaId; // Opcional: si el pago liquida una reserva previa

    // Identificador único del intento de compra (UUID generado por el navegador).
    // Si se repite, el servidor devuelve la misma orden y no duplica nada.
    @Size(max = 60, message = "La referencia es demasiado larga")
    private String referencia;

    // Datos del comprador: se conservan por compatibilidad con HomeController.
    // El servidor usa los datos del socio que inició sesión.
    private String buyerName;
    private String buyerDocType = "DNI";
    private String buyerDocNum;
    private String buyerEmail;

    @Pattern(regexp = "^(SIMULADO|VISA|MASTERCARD|AMEX)?$", message = "Medio de pago no válido")
    private String cardBrand;

    @Pattern(regexp = "^(\\d{4})?$", message = "Los últimos 4 dígitos no son válidos")
    private String cardLast4;

    private String estado = "PAGADO"; // PAGADO, EN_PROCESO, RECHAZADO, CANCELADO

    @Valid
    private List<@NotNull(message = "El carrito contiene un producto inválido") CompraItemDTO> items = new ArrayList<>();
}
