package pe.edu.utp.Grupo04.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.BeneficioPlan;

import java.util.List;
import java.util.stream.Collectors;


@Entity
@Table(name = "planes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(length = 1000)
    private String beneficios;

    @Column(nullable = false)
    private Integer duracionDias;

    @Column(nullable = false)
    private Double precio;

    private Boolean activo = true;

    @Transient
    public List<BeneficioPlan> getListaBeneficios() {
        return BeneficioPlan.parsear(beneficios, nombre);
    }

    @Transient
    public String getBeneficiosCodigos() {
        return getListaBeneficios().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Transient
    public boolean tieneBeneficio(BeneficioPlan beneficio) {
        return beneficio != null && getListaBeneficios().contains(beneficio);
    }
}