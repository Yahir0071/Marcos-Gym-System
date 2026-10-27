package pe.edu.utp.Grupo04.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraItemDTO {

    private Long id;
    private String name;
    private String category;
    private String img;
    private Double price;
    private Integer qty;
}
