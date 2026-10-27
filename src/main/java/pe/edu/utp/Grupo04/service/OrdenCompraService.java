package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.dto.CompraRequestDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato de servicio para la gestión del ciclo de vida de compras, pedidos y boletas electrónicas de prueba.
 * Cumple con los principios SOLID (Single Responsibility, Dependency Inversion e Interface Segregation).
 */
public interface OrdenCompraService {

    /**
     * Procesa y registra una orden de compra (producto o membresía) generando su boleta del prototipo.
     */
    Map<String, Object> registrarCompra(CompraRequestDTO request, String usernameAutenticado);

    /**
     * Busca una orden por su código identificador único (#PED-XXXXX).
     */
    Optional<Map<String, Object>> buscarPorCodigo(String codigo);

    /**
     * Busca una orden por su código o devuelve la orden más reciente por defecto.
     */
    Map<String, Object> buscarPorCodigoODefault(String codigo);

    /**
     * Lista todas las órdenes registradas en el sistema (utilizado por el Módulo Administrador - RF22).
     */
    List<Map<String, Object>> listarTodas();

    /**
     * Lista las órdenes filtradas por un socio específico (utilizado por el Módulo Cliente - RF20).
     */
    List<Map<String, Object>> listarPorCliente(String usernameOEmailODni);

    /**
     * Aprueba una orden que se encontraba en estado EN_PROCESO.
     */
    boolean aprobarCompra(String codigo);

    /**
     * Cambia el estado de entrega a ENTREGADO cuando el cliente recoge el producto (RF14 / RF22).
     */
    boolean marcarEntregado(String codigo);

    /**
     * Genera la previsualización del siguiente código correlativo de pedido (#PED-XXXXX).
     */
    String obtenerProximoCodigo();

    /**
     * Retorna la cantidad total de pedidos registrados.
     */
    int contarTotalOrdenes();
}
