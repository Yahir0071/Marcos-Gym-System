package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoService {
    List<Producto> listarCatalogoActivo();
    List<Producto> listarTodos();
    Optional<Producto> buscarPorId(Long id);
    Producto guardar(Producto producto);
    void cambiarEstado(Long id, boolean activo);
    boolean hayStockSuficiente(Long id, int cantidad);
    void descontarStock(Long id, int cantidad);
    void restaurarStock(Long id, int cantidad);
}
