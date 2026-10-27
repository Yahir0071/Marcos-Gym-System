package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.service.impl.OrdenCompraServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdenCompraServiceTest {

    @Mock
    private SocioService socioService;

    @Mock
    private ProductoService productoService;

    @Mock
    private PlanService planService;

    @Mock
    private MembresiaService membresiaService;

    @InjectMocks
    private OrdenCompraServiceImpl ordenCompraService;

    private Socio socioPrueba;
    private Producto productoPrueba;
    private Plan planPrueba;

    @BeforeEach
    void setUp() {
        socioPrueba = new Socio();
        socioPrueba.setId(10L);
        socioPrueba.setNombres("Carlos");
        socioPrueba.setApellidos("Mendoza Rojas");
        socioPrueba.setDni("72563123");

        productoPrueba = new Producto();
        productoPrueba.setId(1L);
        productoPrueba.setNombre("Whey Protein 2 LB");
        productoPrueba.setPrecio(149.00);
        productoPrueba.setStock(20);

        planPrueba = new Plan();
        planPrueba.setId(2L);
        planPrueba.setNombre("Plan Plus");
        planPrueba.setPrecio(119.00);
        planPrueba.setDuracionDias(30);
    }

    @Test
    @DisplayName("RF18 / RF21 - Debe registrar compra de productos, calcular IGV, correlativo y descontar stock")
    void testRegistrarCompra_Productos_Exitoso() {
        when(socioService.buscarPorUsername("carlos")).thenReturn(Optional.of(socioPrueba));
        when(productoService.buscarPorId(1L)).thenReturn(Optional.of(productoPrueba));

        CompraRequestDTO req = new CompraRequestDTO();
        req.setTipoConcepto("PRODUCTO");
        req.setBuyerName("Carlos Mendoza");
        req.setBuyerDocType("DNI");
        req.setBuyerDocNum("72563123");
        req.setBuyerEmail("carlos@ejemplo.com");
        req.setCardBrand("VISA");
        req.setCardLast4("4242");
        req.setEstado("PAGADO");
        req.setItems(List.of(new CompraItemDTO(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 2)));

        Map<String, Object> orden = ordenCompraService.registrarCompra(req, "carlos");

        assertNotNull(orden);
        assertTrue(orden.get("codigo").toString().startsWith("#PED-"));
        assertTrue(orden.get("boleta").toString().startsWith("B001-"));
        assertEquals("PAGADO", orden.get("estadoPago"));
        assertEquals("POR RECOGER EN GIMNASIO", orden.get("estadoEntrega"));

        // Total 149 * 2 = 298.00
        assertEquals(298.00, ((Number) orden.get("total")).doubleValue());
        assertNotNull(orden.get("base"));
        assertNotNull(orden.get("igv"));
        assertEquals("PROTOTIPO — SIN VALIDEZ TRIBUTARIA", orden.get("leyendaLegal"));

        verify(productoService, times(1)).descontarStock(eq(1L), eq(2));
    }

    @Test
    @DisplayName("RF18 - Debe registrar pago online de membresía y activar plan del socio")
    void testRegistrarCompra_Membresia_Exitoso() {
        when(socioService.buscarPorUsername("carlos")).thenReturn(Optional.of(socioPrueba));
        when(planService.buscarPorId(2L)).thenReturn(Optional.of(planPrueba));

        CompraRequestDTO req = new CompraRequestDTO();
        req.setTipoConcepto("MEMBRESIA");
        req.setPlanId(2L);
        req.setBuyerName("Carlos Mendoza");
        req.setBuyerDocType("DNI");
        req.setBuyerDocNum("72563123");
        req.setBuyerEmail("carlos@ejemplo.com");
        req.setEstado("PAGADO");

        Map<String, Object> orden = ordenCompraService.registrarCompra(req, "carlos");

        assertNotNull(orden);
        assertEquals("PAGADO", orden.get("estadoPago"));
        assertEquals("ACTIVADA", orden.get("estadoEntrega"));
        assertEquals(119.00, ((Number) orden.get("total")).doubleValue());

        verify(membresiaService, times(1)).asignarPlanASocio(eq(10L), eq(2L), any(LocalDate.class));
    }

    @Test
    @DisplayName("RF14 / RF22 - Debe marcar pedido como entregado en recepción")
    void testMarcarEntregado() {
        when(socioService.buscarPorUsername(any())).thenReturn(Optional.of(socioPrueba));
        when(productoService.buscarPorId(any())).thenReturn(Optional.of(productoPrueba));

        CompraRequestDTO req = new CompraRequestDTO();
        req.setTipoConcepto("PRODUCTO");
        req.setEstado("PAGADO");
        req.setItems(List.of(new CompraItemDTO(1L, "Whey Protein", "Suplementos", "/img/whey.jpg", 149.00, 1)));

        Map<String, Object> orden = ordenCompraService.registrarCompra(req, "carlos");
        String codigo = (String) orden.get("codigo");

        boolean marcado = ordenCompraService.marcarEntregado(codigo);
        assertTrue(marcado);

        Map<String, Object> buscada = ordenCompraService.buscarPorCodigoODefault(codigo);
        assertEquals("ENTREGADO", buscada.get("estadoEntrega"));
    }
}
