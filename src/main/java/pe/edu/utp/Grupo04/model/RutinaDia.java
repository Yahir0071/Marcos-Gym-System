package pe.edu.utp.Grupo04.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "rutina_dias", uniqueConstraints = @UniqueConstraint(columnNames = {"username", "vigente_desde", "dia"}))
@Getter @Setter @NoArgsConstructor
public class RutinaDia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String username;
    @Column(name = "vigente_desde", nullable = false) private LocalDate vigenteDesde;
    @Column(nullable = false) private Integer dia;
    @Column(nullable = false, length = 100) private String sesion;
    @Column(nullable = false, length = 4000) private String descripcion;
}
