package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.repository.ProductoRepository;
import pe.edu.utp.Grupo04.service.ProductoService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarCatalogoActivo() {
        return productoRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return productoRepository.findById(id);
    }

    @Override
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public void cambiarEstado(Long id, boolean activo) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id));
        producto.setActivo(activo);
        productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hayStockSuficiente(Long id, int cantidad) {
        return productoRepository.findById(id)
                .map(p -> p.getStock() >= cantidad && Boolean.TRUE.equals(p.getActivo()))
                .orElse(false);
    }

    @Override
    public void descontarStock(Long id, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero.");
        }
        int filasAfectadas = productoRepository.descontarStockAtomico(id, cantidad);
        if (filasAfectadas == 0) {
            Producto producto = productoRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id));
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre() + ". Disponible: " + producto.getStock());
        }
    }

    @Override
    public void restaurarStock(Long id, int cantidad) {
        if (cantidad <= 0) return;
        int filasAfectadas = productoRepository.restaurarStockAtomico(id, cantidad);
        if (filasAfectadas == 0) {
            throw new RecursoNoEncontradoException("Producto no encontrado con ID: " + id);
        }
    }
}
