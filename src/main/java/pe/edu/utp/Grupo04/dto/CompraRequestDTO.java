package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    private Long reservaId; // Opcional: si el pago liquida una reserva previa

    @NotBlank(message = "El nombre del comprador es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String buyerName;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String buyerDocType = "DNI";

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 8, max = 15, message = "El número de documento debe tener entre 8 y 15 dígitos")
    private String buyerDocNum;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe proporcionar un formato de correo electrónico válido")
    private String buyerEmail;

    private String cardBrand = "VISA";

    private String cardLast4 = "4242";

    private String estado = "PAGADO"; // PAGADO, EN_PROCESO, RECHAZADO, CANCELADO

    private List<CompraItemDTO> items = new ArrayList<>();
}
