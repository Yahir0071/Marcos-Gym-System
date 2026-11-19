package pe.edu.utp.Grupo04.dto;

import jakarta.validation.constraints.Max;
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
public class CompraItemDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long id;

    // name, category, img y price se conservan por compatibilidad,
    // pero el servidor los IGNORA: usa los datos de la base de datos.
    private String name;
    private String category;
    private String img;
    private Double price;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 99, message = "La cantidad máxima por producto es 99")
    private Integer qty;
}