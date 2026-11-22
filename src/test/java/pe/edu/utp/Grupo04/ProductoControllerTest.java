package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pe.edu.utp.Grupo04.controller.ProductoController;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.service.ProductoService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductoControllerTest {

    private final ProductoService productoService = mock(ProductoService.class);
    private final ProductoController controller = new ProductoController(productoService);

    @Test
    void detallePublicoInexistenteMuestraVista404() {
        when(productoService.buscarPorId(19L)).thenReturn(Optional.empty());

        var respuesta = controller.detalleProductoPublico(19L);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatus());
        assertEquals("public/producto-no-disponible", respuesta.getViewName());
        assertEquals("/productos", respuesta.getModel().get("catalogo"));
    }

    @Test
    void detalleClienteNoExponeProductoInactivo() {
        var producto = new Producto();
        producto.setActivo(false);
        when(productoService.buscarPorId(20L)).thenReturn(Optional.of(producto));

        var respuesta = controller.detalleProductoCliente(20L);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatus());
        assertEquals("/cliente/productos", respuesta.getModel().get("catalogo"));
    }

    @Test
    void detallePublicoExponeProductoActivo() {
        var producto = new Producto();
        when(productoService.buscarPorId(21L)).thenReturn(Optional.of(producto));

        var respuesta = controller.detalleProductoPublico(21L);

        assertEquals("public/detalle-producto", respuesta.getViewName());
        assertSame(producto, respuesta.getModel().get("producto"));
    }
}
