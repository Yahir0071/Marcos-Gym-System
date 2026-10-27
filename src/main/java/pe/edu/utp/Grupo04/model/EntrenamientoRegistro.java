package pe.edu.utp.Grupo04.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "entrenamiento_registros", uniqueConstraints = @UniqueConstraint(columnNames = {"username", "fecha"}))
@Getter @Setter @NoArgsConstructor
public class EntrenamientoRegistro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String username;
    @Column(nullable = false) private LocalDate fecha;
}
