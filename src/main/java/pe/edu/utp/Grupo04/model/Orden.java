package pe.edu.utp.Grupo04.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_id")
    private Socio socio;

    @Column(nullable = false, length = 15)
    private String tipoConcepto;

    @Column(nullable = false, length = 15)
    private String estadoPago;

    @Column(length = 40)
    private String estadoEntrega;

    @Column(nullable = false)
    private boolean stockReservado = false;

    private Long planId;
    private Integer duracionDias;
    private Long reservaId;

    @Column(nullable = false, length = 150) private String compradorNombre;
    @Column(nullable = false, length = 15)  private String compradorDocTipo;
    @Column(nullable = false, length = 15)  private String compradorDocNum;
    @Column(nullable = false, length = 150) private String compradorEmail;

    @Column(length = 12) private String cardBrand;
    @Column(length = 4)  private String cardLast4;

    @Column(nullable = false)
    private Double total;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false)
    private boolean demo = false;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(Long productoId, String nombre, String categoria,
                               String imagenUrl, double precioUnitario, int cantidad) {
        OrdenDetalle d = new OrdenDetalle();
        d.setOrden(this);
        d.setProductoId(productoId);
        d.setNombre(nombre);
        d.setCategoria(categoria);
        d.setImagenUrl(imagenUrl);
        d.setPrecioUnitario(precioUnitario);
        d.setCantidad(cantidad);
        detalles.add(d);
    }

    @Transient
    public String getCodigo() { return String.format("#PED-%05d", id); }

    @Transient
    public String getBoleta() { return String.format("B001-%08d", id); }
}