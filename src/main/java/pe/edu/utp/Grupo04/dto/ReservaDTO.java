package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaDTO {

    @NotNull(message = "Debe seleccionar un producto")
    private Long productoId;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad mínima para reservar es 1")
    private Integer cantidad = 1;
}
