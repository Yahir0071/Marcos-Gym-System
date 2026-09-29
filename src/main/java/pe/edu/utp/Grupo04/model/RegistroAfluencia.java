package pe.edu.utp.Grupo04.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "registros_afluencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAfluencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    private FranjaHoraria franjaHoraria;

    private Integer concurrenciaAprox; // Cantidad de personas ingresada por el admin

    private LocalDateTime fechaRegistro = LocalDateTime.now();
}