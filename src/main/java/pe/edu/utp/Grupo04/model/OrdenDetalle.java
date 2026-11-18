package pe.edu.utp.Grupo04.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orden_detalles")
@Getter
@Setter
@NoArgsConstructor
public class OrdenDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    private Long productoId;

    @Column(nullable = false, length = 200) private String nombre;
    @Column(length = 40)  private String categoria;
    @Column(length = 255) private String imagenUrl;

    @Column(nullable = false) private Double precioUnitario;
    @Column(nullable = false) private Integer cantidad;

    @Transient
    public double getSubtotal() {
        return Math.round(precioUnitario * cantidad * 100.0) / 100.0;
    }
}