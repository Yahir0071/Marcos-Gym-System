package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public List<Producto> listarCatalogoActivo() {
        return productoRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    @Override
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public void cambiarEstado(Long id, boolean activo) {
        productoRepository.findById(id).ifPresent(p -> {
            p.setActivo(activo);
            productoRepository.save(p);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hayStockSuficiente(Long id, int cantidad) {
        return productoRepository.findById(id)
                .map(p -> p.getStock() >= cantidad && p.getActivo())
                .orElse(false);
    }

    @Override
    public void descontarStock(Long id, int cantidad) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        if (producto.getStock() < cantidad) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }

        producto.setStock(producto.getStock() - cantidad);
        productoRepository.save(producto);
    }

    @Override
    public void restaurarStock(Long id, int cantidad) {
        productoRepository.findById(id).ifPresent(p -> {
            p.setStock(p.getStock() + cantidad);
            productoRepository.save(p);
        });
    }
}
