package pe.edu.utp.Grupo04.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.exception.StockInsuficienteException;
import pe.edu.utp.Grupo04.model.*;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.repository.OrdenRepository;
import pe.edu.utp.Grupo04.service.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Compras simuladas persistentes. Los pedidos se guardan en la base de datos
 * (tablas ordenes y orden_detalles) y sobreviven a un reinicio.
 */
@Slf4j
@Service
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private static final Set<String> ESTADOS = Set.of("PAGADO", "EN_PROCESO", "RECHAZADO", "CANCELADO");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-PE"));

    private final OrdenRepository ordenRepository;
    private final SocioService socioService;
    private final ProductoService productoService;
    private final PlanService planService;
    private final MembresiaService membresiaService;
    private final ReservaService reservaService;

    public OrdenCompraServiceImpl(OrdenRepository ordenRepository,
                                  SocioService socioService,
                                  ProductoService productoService,
                                  PlanService planService,
                                  MembresiaService membresiaService,
                                  ReservaService reservaService) {
        this.ordenRepository = ordenRepository;
        this.socioService = socioService;
        this.productoService = productoService;
        this.planService = planService;
        this.membresiaService = membresiaService;
        this.reservaService = reservaService;
    }

    // =====================================================================
    // REGISTRAR COMPRA
    // =====================================================================
    @Override
    @Transactional
    public Map<String, Object> registrarCompra(CompraRequestDTO req, String username) {
        // 1) Identidad: siempre desde la sesión
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new AccessDeniedException("Debes iniciar sesión para comprar");
        }
        Socio socio = socioService.buscarPorUsername(username)
                .orElseThrow(() -> new AccessDeniedException("No existe un socio asociado a tu sesión"));

        // 2) Duplicados: misma referencia => misma orden, sin repetir efectos
        String referencia = (req.getReferencia() == null || req.getReferencia().isBlank())
                ? UUID.randomUUID().toString()
                : req.getReferencia().trim();
        Optional<Orden> previa = ordenRepository.findByReferencia(referencia);
        if (previa.isPresent()) {
            if (!previa.get().getSocio().getId().equals(socio.getId())) {
                throw new AccessDeniedException("La referencia pertenece a otra cuenta");
            }
            return aMapa(previa.get());
        }

        String estado = req.getEstado() == null ? "PAGADO" : req.getEstado().toUpperCase();
        if (!ESTADOS.contains(estado)) {
            throw new IllegalArgumentException("Estado de pago no válido");
        }
        String tipo = "MEMBRESIA".equalsIgnoreCase(req.getTipoConcepto()) ? "MEMBRESIA" : "PRODUCTO";

        Orden o = new Orden();
        o.setReferencia(referencia);
        o.setSocio(socio);
        o.setTipoConcepto(tipo);
        o.setEstadoPago(estado);
        o.setReservaId(req.getReservaId());
        o.setCompradorNombre((socio.getNombres() + " " + socio.getApellidos()).trim());
        o.setCompradorDocTipo("DNI");
        o.setCompradorDocNum(socio.getDni());
        o.setCompradorEmail(socio.getUsuario() != null ? socio.getUsuario().getUsername() : username);
        o.setCardBrand(req.getCardBrand() == null || req.getCardBrand().isBlank() ? "VISA" : req.getCardBrand());
        o.setCardLast4(req.getCardLast4() == null || req.getCardLast4().isBlank() ? "4242" : req.getCardLast4());

        // 3) Conceptos y precios SIEMPRE desde la base de datos
        if ("MEMBRESIA".equals(tipo)) {
            if (req.getPlanId() == null) {
                throw new IllegalArgumentException("Debe indicar un plan");
            }
            Plan plan = planService.buscarPorId(req.getPlanId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El plan seleccionado no existe"));
            o.setPlanId(plan.getId());
            o.setDuracionDias(plan.getDuracionDias());
            o.agregarDetalle(null,
                    "Membresía " + plan.getNombre() + " (" + plan.getDuracionDias() + " días)",
                    "Membresías", "/img/login-bg.jpg", plan.getPrecio(), 1);
        } else {
            if (req.getItems() == null || req.getItems().isEmpty()) {
                throw new IllegalArgumentException("El carrito está vacío");
            }
            // Une productos repetidos ANTES de comprobar el stock
            Map<Long, Integer> pedidos = new LinkedHashMap<>();
            for (CompraItemDTO it : req.getItems()) {
                pedidos.merge(it.getId(), it.getQty(), Integer::sum);
            }
            for (Map.Entry<Long, Integer> e : pedidos.entrySet()) {
                Producto p = productoService.buscarPorId(e.getKey())
                        .filter(x -> Boolean.TRUE.equals(x.getActivo()))
                        .orElseThrow(() -> new RecursoNoEncontradoException("Producto no disponible: " + e.getKey()));
                int stock = p.getStock() == null ? 0 : p.getStock();
                if (stock < e.getValue()) {
                    throw new StockInsuficienteException(p.getNombre(), stock);   // se rechaza TODO
                }
                o.agregarDetalle(p.getId(), p.getNombre(), "Productos",
                        p.getImagenUrl() != null ? p.getImagenUrl() : "/img/whey.jpg",
                        p.getPrecio(), e.getValue());
            }
        }
        double total = o.getDetalles().stream().mapToDouble(OrdenDetalle::getSubtotal).sum();
        o.setTotal(Math.round(total * 100.0) / 100.0);

        // 4) Efecto según el escenario de la simulación
        switch (estado) {
            case "PAGADO" -> {
                aplicarEfectos(o, socio);
                o.setEstadoEntrega(entregaPagado(tipo));
            }
            case "EN_PROCESO" -> {
                reservarStock(o);
                o.setEstadoEntrega("EN VALIDACIÓN BANCARIA");
            }
            default -> o.setEstadoEntrega("SIN CARGO");   // RECHAZADO o CANCELADO: sin efectos
        }

        // 5) Guardar (si algo falla antes de aquí, la transacción lo deshace todo)
        Orden guardada = ordenRepository.saveAndFlush(o);
        log.info("Orden {} registrada con estado {} para el socio {}", guardada.getCodigo(), estado, socio.getId());
        return aMapa(guardada);
    }

    // =====================================================================
    // APROBAR / CANCELAR UNA OPERACIÓN PENDIENTE
    // =====================================================================
    @Override
    @Transactional
    public Map<String, Object> aprobarCompra(String codigo, String username) {
        Orden o = obtenerPropia(codigo, username);
        if ("EN_PROCESO".equals(o.getEstadoPago())) {   // solo si sigue pendiente: repetir no hace nada
            aplicarEfectos(o, o.getSocio());
            o.setEstadoPago("PAGADO");
            o.setEstadoEntrega(entregaPagado(o.getTipoConcepto()));
        }
        return aMapa(o);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelarCompra(String codigo, String username) {
        Orden o = obtenerPropia(codigo, username);
        if ("EN_PROCESO".equals(o.getEstadoPago())) {
            liberarReserva(o);                          // devuelve el stock solo si estaba reservado
            o.setEstadoPago("CANCELADO");
            o.setEstadoEntrega("SIN CARGO");
        }
        return aMapa(o);
    }

    // =====================================================================
    // CONSULTAS
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> buscarPorCodigoYPropietario(String codigo, String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(aMapa(obtenerPropia(codigo, username)));
        } catch (RecursoNoEncontradoException e) {
            return Optional.empty();   // mismo resultado si no existe o es de otra persona
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> buscarPorCodigo(String codigo) {
        try {
            return ordenRepository.findById(parsearCodigo(codigo)).map(this::aMapa);
        } catch (RecursoNoEncontradoException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarTodas() {
        return ordenRepository.findAllByOrderByFechaDesc().stream()
                .filter(this::esHistorial)
                .map(this::aMapa)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPorCliente(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return listarTodas();
        }
        String f = filtro.trim().toLowerCase();
        return ordenRepository.findAllByOrderByFechaDesc().stream()
                .filter(this::esHistorial)
                .filter(o -> o.getCompradorEmail().toLowerCase().contains(f)
                        || o.getCompradorDocNum().toLowerCase().contains(f)
                        || o.getCompradorNombre().toLowerCase().contains(f))
                .map(this::aMapa)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDeSocio(String username) {
        return socioService.buscarPorUsername(username)
                .map(s -> ordenRepository.findBySocioIdOrderByFechaDesc(s.getId()).stream()
                        .filter(this::esHistorial)
                        .map(this::aMapa)
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional
    public boolean marcarEntregado(String codigo) {
        Optional<Orden> opt;
        try {
            opt = ordenRepository.findById(parsearCodigo(codigo));
        } catch (RecursoNoEncontradoException e) {
            return false;
        }
        if (opt.isEmpty() || !"PAGADO".equals(opt.get().getEstadoPago())) {
            return false;
        }
        Orden o = opt.get();
        o.setEstadoEntrega("ENTREGADO");
        if (o.getReservaId() != null) {
            reservaService.actualizarEstado(o.getReservaId(), EstadoReserva.ENTREGADA);
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerProximoCodigo() {
        return String.format("#PED-%05d", ordenRepository.maxId() + 1);
    }

    @Override
    @Transactional(readOnly = true)
    public int contarTotalOrdenes() {
        return (int) ordenRepository.count();
    }

    // =====================================================================
    // AUXILIARES
    // =====================================================================

    /** El historial muestra pedidos pagados o pendientes. Los demás igual quedan guardados. */
    private boolean esHistorial(Orden o) {
        return "PAGADO".equals(o.getEstadoPago()) || "EN_PROCESO".equals(o.getEstadoPago());
    }

    private String entregaPagado(String tipo) {
        return "MEMBRESIA".equals(tipo) ? "ACTIVADA" : "POR RECOGER EN GIMNASIO";
    }

    /** Convierte "#PED-00007" en 7. Cualquier otro formato se trata como "no existe". */
    private Long parsearCodigo(String codigo) {
        if (codigo == null || !codigo.matches("^#PED-\\d{1,9}$")) {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }
        return Long.parseLong(codigo.substring(5));
    }

    /** Busca el pedido SOLO si pertenece a ese usuario. */
    private Orden obtenerPropia(String codigo, String username) {
        Long id = parsearCodigo(codigo);
        Socio socio = socioService.buscarPorUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        return ordenRepository.findByIdAndSocioId(id, socio.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
    }

    /** Aplica los efectos de una aprobación. Se ejecuta UNA sola vez por orden. */
    private void aplicarEfectos(Orden o, Socio socio) {
        if ("MEMBRESIA".equals(o.getTipoConcepto())) {
            membresiaService.asignarPlanASocio(socio.getId(), o.getPlanId(), LocalDate.now());
        } else if (o.getReservaId() != null) {
            // El stock ya se descontó al hacer la reserva: solo se confirma
            reservaService.actualizarEstado(o.getReservaId(), EstadoReserva.CONFIRMADA);
        } else if (!o.isStockReservado()) {
            // Si ya estaba reservado (pendiente) NO se vuelve a descontar
            for (OrdenDetalle d : o.getDetalles()) {
                productoService.descontarStock(d.getProductoId(), d.getCantidad());
            }
        }
        o.setStockReservado(false);
    }

    private void reservarStock(Orden o) {
        if (o.getReservaId() != null || "MEMBRESIA".equals(o.getTipoConcepto())) {
            return;
        }
        for (OrdenDetalle d : o.getDetalles()) {
            productoService.descontarStock(d.getProductoId(), d.getCantidad());
        }
        o.setStockReservado(true);
    }

    private void liberarReserva(Orden o) {
        if (!o.isStockReservado()) {
            return;
        }
        for (OrdenDetalle d : o.getDetalles()) {
            productoService.restaurarStock(d.getProductoId(), d.getCantidad());
        }
        o.setStockReservado(false);
    }

    /** Mantiene el mismo formato de Map que ya usan las plantillas Thymeleaf. */
    private Map<String, Object> aMapa(Orden o) {
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> resumen = new ArrayList<>();
        int unidades = 0;
        for (OrdenDetalle d : o.getDetalles()) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("id", d.getProductoId());
            it.put("name", d.getNombre());
            it.put("category", d.getCategoria());
            it.put("img", d.getImagenUrl());
            it.put("price", d.getPrecioUnitario());
            it.put("priceFormatted", String.format(Locale.US, "%.2f", d.getPrecioUnitario()));
            it.put("qty", d.getCantidad());
            it.put("sub", d.getSubtotal());
            it.put("subFormatted", String.format(Locale.US, "%.2f", d.getSubtotal()));
            items.add(it);
            unidades += d.getCantidad();
            resumen.add(d.getCantidad() + " × " + d.getNombre());
        }
        double total = o.getTotal();
        double base = Math.round((total / 1.18) * 100.0) / 100.0;
        double igv = Math.round((total - base) * 100.0) / 100.0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numero", o.getId().intValue());
        m.put("codigo", o.getCodigo());
        m.put("boleta", o.getBoleta());
        m.put("operacion", String.format("OP-SIM-%05d", o.getId()));
        m.put("referencia", o.getReferencia());
        m.put("fecha", o.getFecha().format(FMT));
        m.put("estadoPago", o.getEstadoPago());
        m.put("estadoEntrega", o.getEstadoEntrega());
        m.put("buyerName", o.getCompradorNombre());
        m.put("buyerDocType", o.getCompradorDocTipo());
        m.put("buyerDocNum", o.getCompradorDocNum());
        m.put("buyerEmail", o.getCompradorEmail());
        m.put("cardBrand", o.getCardBrand());
        m.put("cardLast4", o.getCardLast4());
        m.put("tipoConcepto", o.getTipoConcepto());
        m.put("items", items);
        m.put("totalUnidades", unidades);
        m.put("resumenItems", String.join(" • ", resumen));
        m.put("base", base);
        m.put("igv", igv);
        m.put("total", total);
        m.put("baseFormatted", String.format(Locale.US, "%.2f", base));
        m.put("igvFormatted", String.format(Locale.US, "%.2f", igv));
        m.put("totalFormatted", String.format(Locale.US, "%.2f", total));
        m.put("leyendaLegal", "SIMULACIÓN ACADÉMICA — SIN VALIDEZ TRIBUTARIA");
        if (o.getReservaId() != null) {
            m.put("reservaId", o.getReservaId());
        }
        return m;
    }
}