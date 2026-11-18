package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.Grupo04.model.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();

    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.id = :id AND p.stock >= :cantidad")
    int descontarStockAtomico(@Param("id") Long id, @Param("cantidad") int cantidad);

    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.id = :id")
    int restaurarStockAtomico(@Param("id") Long id, @Param("cantidad") int cantidad);
}
