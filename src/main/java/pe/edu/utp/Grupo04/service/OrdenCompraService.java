package pe.edu.utp.Grupo04.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato del servicio de compras SIMULADAS (proyecto académico, sin cobros reales).
 */
public interface OrdenCompraService {

    /** Registra una compra. El comprador sale de la sesión; precios y stock, de la BD. */
    Map<String, Object> registrarCompra(CompraRequestDTO request, String usernameAutenticado);

    Map<String, Object> registrarCompraDeInscripcion(CompraRequestDTO request, String username, double importe);

    /** Búsqueda exacta por código (uso administrativo). Nunca devuelve "otro" pedido. */
    Optional<Map<String, Object>> buscarPorCodigo(String codigo);

    /** Búsqueda por código Y propietario. Vacío si no existe o es de otra persona. */
    Optional<Map<String, Object>> buscarPorCodigoYPropietario(String codigo, String username);

    List<Map<String, Object>> listarTodas();

    Page<Map<String, Object>> listarPagina(Pageable pageable);

    Page<Map<String, Object>> listarPaginaPorCliente(String filtro, Pageable pageable);

    /** Filtro libre por correo, DNI o nombre (uso administrativo). */
    List<Map<String, Object>> listarPorCliente(String filtro);

    /** Pedidos del socio que inició sesión. */
    List<Map<String, Object>> listarDeSocio(String username);

    Map<String, Object> aprobarCompra(String codigo, String username);

    Map<String, Object> cancelarCompra(String codigo, String username);

    boolean marcarEntregado(String codigo);

    String obtenerProximoCodigo();

    int contarTotalOrdenes();
}
