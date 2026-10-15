package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.repository.ProductoRepository;
import pe.edu.utp.Grupo04.service.impl.ProductoServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Producto productoConStock;
    private Producto productoSinStock;

    @BeforeEach
    void setUp() {
        productoConStock = new Producto();
        productoConStock.setId(10L);
        productoConStock.setNombre("Whey Protein 2 LB");
        productoConStock.setPrecio(149.00);
        productoConStock.setStock(15);
        productoConStock.setActivo(true);

        productoSinStock = new Producto();
        productoSinStock.setId(11L);
        productoSinStock.setNombre("Creatina 300g");
        productoSinStock.setPrecio(89.00);
        productoSinStock.setStock(0);
        productoSinStock.setActivo(true);
    }

    @Test
    @DisplayName("RF13/RF14 - Debe confirmar stock suficiente cuando la cantidad solicitada es menor o igual al inventario")
    void testHayStockSuficiente_Verdadero() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoConStock));

        boolean disponible = productoService.hayStockSuficiente(10L, 5);

        assertTrue(disponible, "Debe retornar true ya que 5 es menor a 15 de stock");
        verify(productoRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("RF13/RF14 - Debe denegar stock suficiente cuando no hay unidades disponibles")
    void testHayStockSuficiente_Falso() {
        when(productoRepository.findById(11L)).thenReturn(Optional.of(productoSinStock));

        boolean disponible = productoService.hayStockSuficiente(11L, 1);

        assertFalse(disponible, "Debe retornar false ya que el stock disponible es 0");
        verify(productoRepository, times(1)).findById(11L);
    }

    @Test
    @DisplayName("RF13 - Debe descontar stock exitosamente al realizar una reserva")
    void testDescontarStock_Exitoso() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoConStock));
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        productoService.descontarStock(10L, 3);

        assertEquals(12, productoConStock.getStock(), "El stock debió reducirse de 15 a 12");
        verify(productoRepository, times(1)).save(productoConStock);
    }

    @Test
    @DisplayName("RF13 - Debe lanzar IllegalStateException si se intenta descontar más stock del disponible")
    void testDescontarStock_LanzaExcepcion() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoConStock));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            productoService.descontarStock(10L, 20); // Solicita 20 pero solo hay 15
        });

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    @DisplayName("RF13/RF14 - Debe restaurar stock correctamente al cancelar una reserva")
    void testRestaurarStock_Exitoso() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoConStock));
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        productoService.restaurarStock(10L, 5);

        assertEquals(20, productoConStock.getStock(), "El stock debió aumentar de 15 a 20 tras la restitución");
        verify(productoRepository, times(1)).save(productoConStock);
    }
}
