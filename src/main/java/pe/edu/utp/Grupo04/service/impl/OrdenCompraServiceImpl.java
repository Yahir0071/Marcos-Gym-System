package pe.edu.utp.Grupo04.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación desacoplada y thread-safe del servicio de órdenes y boletas de compra online.
 * Satisface los principios SOLID (SRP, DIP) y coordina inventario con ProductoService, vigencias con MembresiaService
 * y confirmación de reservas con ReservaService.
 */
@Slf4j
@Service
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final SocioService socioService;
    private final ProductoService productoService;
    private final PlanService planService;
    private final MembresiaService membresiaService;
    private final ReservaService reservaService;

    // Persistencia concurrente en memoria para el prototipo en ejecución
    private final List<Map<String, Object>> ordenesMemoria = new CopyOnWriteArrayList<>();
    private final Map<String, Map<String, Object>> ordenesPorCodigo = new ConcurrentHashMap<>();
    private final AtomicInteger secuenciaPedido = new AtomicInteger(521);
    private final AtomicBoolean primeraCompraDemoReemplazada = new AtomicBoolean(false);

    public OrdenCompraServiceImpl(SocioService socioService,
                                  ProductoService productoService,
                                  PlanService planService,
                                  MembresiaService membresiaService,
                                  ReservaService reservaService) {
        this.socioService = socioService;
        this.productoService = productoService;
        this.planService = planService;
        this.membresiaService = membresiaService;
        this.reservaService = reservaService;
    }

    @PostConstruct
    public void inicializarHistorialEnMemoria() {
        if (!ordenesMemoria.isEmpty()) {
            return;
        }

        // 1. Pedido activo inicial #PED-00521
        List<Map<String, Object>> items521 = new ArrayList<>();
        items521.add(crearItemMapa(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 1));
        items521.add(crearItemMapa(2L, "Shaker 700ml", "Botellas", "/img/shaker.jpg", 45.00, 1));
        Map<String, Object> ped521 = construirOrdenMapa(
                521, "20 Ene 2025 · 10:45", "PAGADO", "POR RECOGER EN GIMNASIO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "VISA", "4242", "PRODUCTO", items521
        );

        // 2. Pedidos históricos entregados
        List<Map<String, Object>> items498 = new ArrayList<>();
        items498.add(crearItemMapa(3L, "Creatina Monohidratada 300g", "Suplementos", "/img/creatina.jpg", 95.00, 1));
        Map<String, Object> ped498 = construirOrdenMapa(
                498, "12 Ene 2025 · 17:18", "PAGADO", "ENTREGADO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "VISA", "4242", "PRODUCTO", items498
        );

        List<Map<String, Object>> items462 = new ArrayList<>();
        items462.add(crearItemMapa(2L, "Shaker 700ml", "Botellas", "/img/shaker.jpg", 45.00, 2));
        items462.add(crearItemMapa(4L, "Correas de Levantamiento Par", "Accesorios", "/img/shaker.jpg", 39.00, 1));
        Map<String, Object> ped462 = construirOrdenMapa(
                462, "18 Dic 2024 · 09:05", "PAGADO", "ENTREGADO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "MASTERCARD", "8821", "PRODUCTO", items462
        );

        List<Map<String, Object>> items419 = new ArrayList<>();
        items419.add(crearItemMapa(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 1));
        Map<String, Object> ped419 = construirOrdenMapa(
                419, "04 Dic 2024 · 11:20", "PAGADO", "ENTREGADO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "VISA", "4242", "PRODUCTO", items419
        );

        registrarOrdenEnMemoria(ped521, false);
        registrarOrdenEnMemoria(ped498, false);
        registrarOrdenEnMemoria(ped462, false);
        registrarOrdenEnMemoria(ped419, false);
        log.info("Historial base de órdenes y boletas inicializado con 4 registros de prueba");
    }

    @Override
    public Map<String, Object> registrarCompra(CompraRequestDTO request, String usernameAutenticado) {
        String estadoPago = request.getEstado() != null ? request.getEstado().toUpperCase() : "PAGADO";
        String tipoConcepto = request.getTipoConcepto() != null ? request.getTipoConcepto().toUpperCase() : "PRODUCTO";

        // Obtener socio autenticado si está disponible
        Socio socio = null;
        if (usernameAutenticado != null && !usernameAutenticado.isBlank() && !"anonymousUser".equals(usernameAutenticado)) {
            socio = socioService.buscarPorUsername(usernameAutenticado).orElse(null);
        }

        String buyerName = (socio != null) ? (socio.getNombres() + " " + socio.getApellidos()).trim() : request.getBuyerName();
        String buyerDocType = (socio != null) ? "DNI" : request.getBuyerDocType();
        String buyerDocNum = (socio != null) ? socio.getDni() : request.getBuyerDocNum();
        String buyerEmail = (socio != null && socio.getUsuario() != null) ? socio.getUsuario().getUsername() : request.getBuyerEmail();

        List<Map<String, Object>> itemsProcesados = new ArrayList<>();

        if ("MEMBRESIA".equals(tipoConcepto) && request.getPlanId() != null) {
            // Caso compra/renovación de membresía online (RF18)
            Plan plan = planService.buscarPorId(request.getPlanId()).orElse(null);
            double precioPlan = (plan != null && plan.getPrecio() != null) ? plan.getPrecio() : 120.00;
            String nombrePlan = (plan != null) ? ("Membresía " + plan.getNombre() + " (" + plan.getDuracionDias() + " días)") : "Membresía Plan Estándar";
            itemsProcesados.add(crearItemMapa(request.getPlanId(), nombrePlan, "Membresías", "/img/login-bg.jpg", precioPlan, 1));

            // Si el pago es exitoso y el socio existe, activar membresía en PostgreSQL
            if ("PAGADO".equals(estadoPago) && socio != null && plan != null) {
                try {
                    membresiaService.asignarPlanASocio(socio.getId(), plan.getId(), LocalDate.now());
                    log.info("Membresía {} asignada exitosamente al socio {} tras pago online", plan.getNombre(), socio.getDni());
                } catch (Exception e) {
                    log.warn("No se pudo persistir la membresía en PostgreSQL: {}", e.getMessage());
                }
            }
        } else {
            // Caso compra de productos del catálogo o reserva previa
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                for (CompraItemDTO item : request.getItems()) {
                    Long id = item.getId() != null ? item.getId() : 1L;
                    String name = item.getName() != null ? item.getName() : "Producto";
                    String category = item.getCategory() != null ? item.getCategory() : "Suplementos";
                    String img = item.getImg() != null ? item.getImg() : "/img/whey.jpg";
                    double price = item.getPrice() != null ? item.getPrice() : 0.0;
                    int qty = (item.getQty() != null && item.getQty() > 0) ? item.getQty() : 1;
                    itemsProcesados.add(crearItemMapa(id, name, category, img, price, qty));
                }
            }
            if (itemsProcesados.isEmpty()) {
                itemsProcesados.add(crearItemMapa(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 1));
            }

            // Descontar inventario real si el pago fue aprobado y no proviene de reserva previa ya descontada
            if (("PAGADO".equals(estadoPago) || "EN_PROCESO".equals(estadoPago)) && request.getReservaId() == null) {
                descontarStockDeProductos(itemsProcesados);
            }

            // Si se liquidó una reserva existente mediante pago aprobado, confirmar su estado (RF14)
            if ("PAGADO".equals(estadoPago) && request.getReservaId() != null) {
                try {
                    reservaService.actualizarEstado(request.getReservaId(), EstadoReserva.CONFIRMADA);
                    log.info("Reserva #{} actualizada a CONFIRMADA tras pago online", request.getReservaId());
                } catch (Exception e) {
                    log.warn("No se pudo actualizar estado de reserva #{}: {}", request.getReservaId(), e.getMessage());
                }
            }
        }

        // Numeración correlativa atómica
        int numPedido;
        if ("PAGADO".equals(estadoPago) || "EN_PROCESO".equals(estadoPago)) {
            if (primeraCompraDemoReemplazada.compareAndSet(false, true)) {
                numPedido = 521;
            } else {
                numPedido = secuenciaPedido.incrementAndGet();
            }
        } else {
            numPedido = primeraCompraDemoReemplazada.get() ? secuenciaPedido.get() + 1 : 521;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", new Locale("es", "PE"));
        String fechaActual = LocalDateTime.now().format(fmt);

        String estadoEntrega = "PAGADO".equals(estadoPago)
                ? ("MEMBRESIA".equals(tipoConcepto) ? "ACTIVADA" : "POR RECOGER EN GIMNASIO")
                : ("EN_PROCESO".equals(estadoPago) ? "EN VALIDACIÓN BANCARIA" : "SIN CARGO");

        Map<String, Object> nuevaOrden = construirOrdenMapa(
                numPedido, fechaActual, estadoPago, estadoEntrega,
                buyerName, buyerDocType, buyerDocNum, buyerEmail,
                request.getCardBrand(), request.getCardLast4(), tipoConcepto, itemsProcesados
        );

        if (request.getReservaId() != null) {
            nuevaOrden.put("reservaId", request.getReservaId());
        }

        String codigo = (String) nuevaOrden.get("codigo");
        ordenesPorCodigo.put(codigo, nuevaOrden);

        if ("PAGADO".equals(estadoPago) || "EN_PROCESO".equals(estadoPago)) {
            registrarOrdenEnMemoria(nuevaOrden, true);
        }

        log.info("Orden {} generada con éxito con boleta {}", codigo, nuevaOrden.get("boleta"));
        return nuevaOrden;
    }

    @Override
    public Optional<Map<String, Object>> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ordenesPorCodigo.get(codigo));
    }

    @Override
    public Map<String, Object> buscarPorCodigoODefault(String codigo) {
        if (codigo != null && ordenesPorCodigo.containsKey(codigo)) {
            return ordenesPorCodigo.get(codigo);
        }
        if (!ordenesMemoria.isEmpty()) {
            return ordenesMemoria.get(0);
        }
        return ordenesPorCodigo.get("#PED-00521");
    }

    @Override
    public List<Map<String, Object>> listarTodas() {
        return new ArrayList<>(ordenesMemoria);
    }

    @Override
    public List<Map<String, Object>> listarPorCliente(String usernameOEmailODni) {
        if (usernameOEmailODni == null || usernameOEmailODni.isBlank()) {
            return listarTodas();
        }
        String filtro = usernameOEmailODni.trim().toLowerCase();
        List<Map<String, Object>> filtradas = new ArrayList<>();
        for (Map<String, Object> orden : ordenesMemoria) {
            String email = String.valueOf(orden.getOrDefault("buyerEmail", "")).toLowerCase();
            String dni = String.valueOf(orden.getOrDefault("buyerDocNum", "")).toLowerCase();
            String name = String.valueOf(orden.getOrDefault("buyerName", "")).toLowerCase();
            if (email.contains(filtro) || dni.contains(filtro) || name.contains(filtro)) {
                filtradas.add(orden);
            }
        }
        return filtradas;
    }

    @Override
    public boolean aprobarCompra(String codigo) {
        Map<String, Object> orden = buscarPorCodigoODefault(codigo);
        if (orden != null) {
            orden.put("estadoPago", "PAGADO");
            String tipo = String.valueOf(orden.getOrDefault("tipoConcepto", "PRODUCTO"));
            orden.put("estadoEntrega", "MEMBRESIA".equals(tipo) ? "ACTIVADA" : "POR RECOGER EN GIMNASIO");

            Object resIdObj = orden.get("reservaId");
            if (resIdObj != null) {
                try {
                    Long resId = Long.parseLong(resIdObj.toString());
                    reservaService.actualizarEstado(resId, EstadoReserva.CONFIRMADA);
                } catch (Exception ignored) {
                }
            }

            registrarOrdenEnMemoria(orden, true);
            log.info("Compra {} aprobada exitosamente", codigo);
            return true;
        }
        return false;
    }

    @Override
    public boolean marcarEntregado(String codigo) {
        Map<String, Object> orden = buscarPorCodigoODefault(codigo);
        if (orden != null && "PAGADO".equals(orden.get("estadoPago"))) {
            orden.put("estadoEntrega", "ENTREGADO");

            Object resIdObj = orden.get("reservaId");
            if (resIdObj != null) {
                try {
                    Long resId = Long.parseLong(resIdObj.toString());
                    reservaService.actualizarEstado(resId, EstadoReserva.ENTREGADA);
                } catch (Exception ignored) {
                }
            }

            registrarOrdenEnMemoria(orden, false);
            log.info("Pedido {} marcado como ENTREGADO", codigo);
            return true;
        }
        return false;
    }

    @Override
    public String obtenerProximoCodigo() {
        int num = primeraCompraDemoReemplazada.get() ? secuenciaPedido.get() + 1 : 521;
        return String.format("#PED-%05d", num);
    }

    @Override
    public int contarTotalOrdenes() {
        return ordenesMemoria.size();
    }

    // --- MÉTODOS AUXILIARES ---

    private void descontarStockDeProductos(List<Map<String, Object>> items) {
        List<Producto> todos = productoService.listarTodos();
        for (Map<String, Object> it : items) {
            int qty = ((Number) it.get("qty")).intValue();
            if (qty <= 0) continue;

            Long idBuscado = it.get("id") != null ? ((Number) it.get("id")).longValue() : null;
            String nombreBuscado = it.get("name") != null ? it.get("name").toString().trim() : "";

            Producto encontrado = null;
            if (idBuscado != null) {
                encontrado = productoService.buscarPorId(idBuscado).orElse(null);
            }
            if (encontrado == null && !nombreBuscado.isEmpty()) {
                for (Producto p : todos) {
                    if (p.getNombre() != null && p.getNombre().equalsIgnoreCase(nombreBuscado)) {
                        encontrado = p;
                        break;
                    }
                }
            }
            if (encontrado != null && encontrado.getStock() != null && encontrado.getStock() > 0) {
                int aDescontar = Math.min(qty, encontrado.getStock());
                if (aDescontar > 0) {
                    productoService.descontarStock(encontrado.getId(), aDescontar);
                    log.info("Stock descontado en {} unidades para el producto {}", aDescontar, encontrado.getNombre());
                }
            }
        }
    }

    private Map<String, Object> crearItemMapa(Long id, String name, String category, String img, double price, int qty) {
        Map<String, Object> item = new LinkedHashMap<>();
        double sub = Math.round(price * qty * 100.0) / 100.0;
        item.put("id", id);
        item.put("name", name);
        item.put("category", category);
        item.put("img", img);
        item.put("price", price);
        item.put("priceFormatted", String.format(Locale.US, "%.2f", price));
        item.put("qty", qty);
        item.put("sub", sub);
        item.put("subFormatted", String.format(Locale.US, "%.2f", sub));
        return item;
    }

    private Map<String, Object> construirOrdenMapa(int numero,
                                                   String fecha,
                                                   String estadoPago,
                                                   String estadoEntrega,
                                                   String buyerName,
                                                   String buyerDocType,
                                                   String buyerDocNum,
                                                   String buyerEmail,
                                                   String cardBrand,
                                                   String cardLast4,
                                                   String tipoConcepto,
                                                   List<Map<String, Object>> items) {
        String codigo = String.format("#PED-%05d", numero);
        String boleta = String.format("B001-%08d", numero);
        String operacion = String.format("OP-EJ-%05d", numero);

        double total = 0.0;
        int totalUnidades = 0;
        List<String> resumenPartes = new ArrayList<>();
        for (Map<String, Object> it : items) {
            double sub = ((Number) it.get("sub")).doubleValue();
            int qty = ((Number) it.get("qty")).intValue();
            total += sub;
            totalUnidades += qty;
            resumenPartes.add(qty + " × " + it.get("name"));
        }
        double base = Math.round((total / 1.18) * 100.0) / 100.0;
        double igv = Math.round((total - base) * 100.0) / 100.0;

        Map<String, Object> orden = new LinkedHashMap<>();
        orden.put("numero", numero);
        orden.put("codigo", codigo);
        orden.put("boleta", boleta);
        orden.put("operacion", operacion);
        orden.put("fecha", fecha);
        orden.put("estadoPago", estadoPago);
        orden.put("estadoEntrega", estadoEntrega);
        orden.put("buyerName", buyerName);
        orden.put("buyerDocType", buyerDocType);
        orden.put("buyerDocNum", buyerDocNum);
        orden.put("buyerEmail", buyerEmail);
        orden.put("cardBrand", (cardBrand != null && !cardBrand.isBlank()) ? cardBrand : "VISA");
        orden.put("cardLast4", (cardLast4 != null && !cardLast4.isBlank()) ? cardLast4 : "4242");
        orden.put("tipoConcepto", tipoConcepto);
        orden.put("items", items);
        orden.put("totalUnidades", totalUnidades);
        orden.put("resumenItems", String.join(" • ", resumenPartes));
        orden.put("base", base);
        orden.put("igv", igv);
        orden.put("total", total);
        orden.put("baseFormatted", String.format(Locale.US, "%.2f", base));
        orden.put("igvFormatted", String.format(Locale.US, "%.2f", igv));
        orden.put("totalFormatted", String.format(Locale.US, "%.2f", total));
        orden.put("leyendaLegal", "PROTOTIPO — SIN VALIDEZ TRIBUTARIA");
        return orden;
    }

    private void registrarOrdenEnMemoria(Map<String, Object> orden, boolean alInicio) {
        String codigo = (String) orden.get("codigo");
        ordenesPorCodigo.put(codigo, orden);
        ordenesMemoria.removeIf(o -> codigo.equals(o.get("codigo")));
        if (alInicio) {
            ordenesMemoria.add(0, orden);
        } else {
            ordenesMemoria.add(orden);
        }
    }
}
