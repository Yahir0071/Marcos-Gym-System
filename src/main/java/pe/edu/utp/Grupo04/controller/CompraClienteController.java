package pe.edu.utp.Grupo04.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Reserva;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

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

@Controller
@RequestMapping("/cliente")
public class CompraClienteController {

    private final SocioService socioService;
    private final ProductoService productoService;
    private final ReservaService reservaService;

    // Memoria en ejecución de pedidos y boletas creadas mientras la aplicación esté activa
    private final List<Map<String, Object>> ordenesMemoria = new CopyOnWriteArrayList<>();
    private final Map<String, Map<String, Object>> ordenesPorCodigo = new ConcurrentHashMap<>();
    private final AtomicInteger secuenciaPedido = new AtomicInteger(521);
    private final AtomicBoolean primeraCompraDemoReemplazada = new AtomicBoolean(false);

    public CompraClienteController(SocioService socioService,
                                   ProductoService productoService,
                                   ReservaService reservaService) {
        this.socioService = socioService;
        this.productoService = productoService;
        this.reservaService = reservaService;
        inicializarHistorialEnMemoria();
    }

    private void inicializarHistorialEnMemoria() {
        // Pedido activo inicial #PED-00521 (se actualiza con la primera compra real y luego se crean #PED-00522, #PED-00523...)
        List<Map<String, Object>> items521 = new ArrayList<>();
        items521.add(crearItemMapa(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 1));
        items521.add(crearItemMapa(2L, "Shaker 700ml", "Botellas", "/img/shaker.jpg", 45.00, 1));
        Map<String, Object> ped521 = construirOrdenMapa(
                521, "20 Ene 2025 · 10:45", "PAGADO", "POR RECOGER EN GIMNASIO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "VISA", "4242", items521
        );

        // Pedidos históricos entregados
        List<Map<String, Object>> items498 = new ArrayList<>();
        items498.add(crearItemMapa(3L, "Creatina Monohidratada 300g", "Suplementos", "/img/creatina.jpg", 95.00, 1));
        Map<String, Object> ped498 = construirOrdenMapa(
                498, "12 Ene 2025 · 17:18", "PAGADO", "ENTREGADO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "VISA", "4242", items498
        );

        List<Map<String, Object>> items462 = new ArrayList<>();
        items462.add(crearItemMapa(2L, "Shaker 700ml", "Botellas", "/img/shaker.jpg", 45.00, 2));
        items462.add(crearItemMapa(4L, "Correas de Levantamiento Par", "Accesorios", "/img/shaker.jpg", 39.00, 1));
        Map<String, Object> ped462 = construirOrdenMapa(
                462, "18 Dic 2024 · 09:05", "PAGADO", "ENTREGADO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "MASTERCARD", "8821", items462
        );

        List<Map<String, Object>> items419 = new ArrayList<>();
        items419.add(crearItemMapa(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 1));
        Map<String, Object> ped419 = construirOrdenMapa(
                419, "04 Dic 2024 · 11:20", "PAGADO", "ENTREGADO",
                "Carlos Mendoza Rojas", "DNI", "72563123", "carlos.mendoza@ejemplo.com",
                "VISA", "4242", items419
        );

        registrarOrdenEnMemoria(ped521, false);
        registrarOrdenEnMemoria(ped498, false);
        registrarOrdenEnMemoria(ped462, false);
        registrarOrdenEnMemoria(ped419, false);
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
        double base = total / 1.18;
        double igv = total - base;

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
        orden.put("cardBrand", cardBrand);
        orden.put("cardLast4", cardLast4);
        orden.put("items", items);
        orden.put("totalUnidades", totalUnidades);
        orden.put("resumenItems", String.join(" • ", resumenPartes));
        orden.put("base", base);
        orden.put("igv", igv);
        orden.put("total", total);
        orden.put("baseFormatted", String.format(Locale.US, "%.2f", base));
        orden.put("igvFormatted", String.format(Locale.US, "%.2f", igv));
        orden.put("totalFormatted", String.format(Locale.US, "%.2f", total));
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

    private String obtenerProximoCodigo() {
        int num = primeraCompraDemoReemplazada.get() ? secuenciaPedido.get() + 1 : 521;
        return String.format("#PED-%05d", num);
    }

    private Map<String, Object> buscarOrdenPorCodigoODefault(String codigo) {
        if (codigo != null && ordenesPorCodigo.containsKey(codigo)) {
            return ordenesPorCodigo.get(codigo);
        }
        if (!ordenesMemoria.isEmpty()) {
            return ordenesMemoria.get(0);
        }
        return ordenesPorCodigo.get("#PED-00521");
    }

    private void cargarDatosSocio(Authentication auth, Model model) {
        Socio socio = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            socio = socioService.buscarPorUsername(auth.getName()).orElse(null);
        }
        if (socio == null) {
            socio = new Socio();
            socio.setNombres("Carlos");
            socio.setApellidos("Mendoza Rojas");
            socio.setDni("72563123");
            socio.setTelefono("987654321");
        }
        model.addAttribute("socio", socio);
        model.addAttribute("productosCatalogo", productoService.listarCatalogoActivo());
        model.addAttribute("ordenesMemoria", ordenesMemoria);
        model.addAttribute("proximoCodigoPedido", obtenerProximoCodigo());
    }

    private void descontarStockDeProductos(List<Map<String, Object>> items) {
        List<Producto> todos = productoService.listarTodos();
        for (Map<String, Object> it : items) {
            int qty = ((Number) it.get("qty")).intValue();
            if (qty <= 0) {
                continue;
            }

            Long idBuscado = it.get("id") != null ? ((Number) it.get("id")).longValue() : null;
            String nombreBuscado = it.get("name") != null ? it.get("name").toString().trim() : "";

            Producto encontrado = null;
            if (idBuscado != null) {
                Optional<Producto> opt = productoService.buscarPorId(idBuscado);
                if (opt.isPresent()) {
                    encontrado = opt.get();
                }
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
                }
            }
        }
    }

    private String categoriaProducto(Producto producto) {
        String nombre = producto.getNombre() == null ? "" : producto.getNombre().toUpperCase(Locale.ROOT);
        return nombre.contains("SHAKER") || nombre.contains("BOTELLA") ? "Botellas" : "Suplementos";
    }

    // --- API REST EN MEMORIA PARA CREAR COMPRAS / BOLETAS Y DESCONTAR STOCK ---
    @PostMapping("/api/compras")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> registrarCompraApi(@RequestBody Map<String, Object> payload,
                                                Authentication auth) {
        String estadoPago = payload.getOrDefault("estado", "PAGADO").toString().toUpperCase();
        String buyerName = payload.getOrDefault("buyerName", "Carlos Mendoza Rojas").toString();
        String buyerDocType = payload.getOrDefault("buyerDocType", "DNI").toString();
        String buyerDocNum = payload.getOrDefault("buyerDocNum", "72563123").toString();
        String buyerEmail = payload.getOrDefault("buyerEmail", "carlos.mendoza@ejemplo.com").toString();
        String cardBrand = payload.getOrDefault("cardBrand", "VISA").toString();
        String cardLast4 = payload.getOrDefault("cardLast4", "4242").toString();

        Reserva reservaPago = null;
        Long reservaId = null;
        if (payload.get("reservaId") != null) {
            try {
                reservaId = Long.parseLong(payload.get("reservaId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "La reserva indicada no es válida."));
            }
            reservaPago = reservaService.buscarPorId(reservaId).orElse(null);
            if (reservaPago == null || auth == null
                    || !auth.getName().equals(reservaPago.getSocio().getUsuario().getUsername())) {
                return ResponseEntity.notFound().build();
            }
            if (reservaPago.getEstado() != EstadoReserva.PENDIENTE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Esta reserva ya no está pendiente de pago."));
            }
        }

        List<Map<String, Object>> itemsProcesados = new ArrayList<>();
        if (reservaPago != null) {
            Producto producto = reservaPago.getProducto();
            String imagen = producto.getImagenUrl() == null || producto.getImagenUrl().isBlank()
                    ? "/img/whey.jpg"
                    : producto.getImagenUrl();
            itemsProcesados.add(crearItemMapa(producto.getId(), producto.getNombre(),
                    categoriaProducto(producto), imagen, producto.getPrecio(), reservaPago.getCantidad()));
        } else {
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) payload.get("items");
            if (rawItems != null) {
                for (Map<String, Object> raw : rawItems) {
                    Long id = 1L;
                    try {
                        if (raw.get("id") != null) {
                            id = Long.parseLong(raw.get("id").toString());
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    String name = raw.getOrDefault("name", "Producto").toString();
                    String category = raw.getOrDefault("category", "Suplementos").toString();
                    String img = raw.getOrDefault("img", "/img/whey.jpg").toString();
                    double price = 0.0;
                    int qty = 1;
                    try {
                        price = Double.parseDouble(raw.getOrDefault("price", "0").toString());
                        qty = Integer.parseInt(raw.getOrDefault("qty", "1").toString());
                    } catch (NumberFormatException ignored) {
                    }
                    itemsProcesados.add(crearItemMapa(id, name, category, img, price, qty));
                }
            }
            if (itemsProcesados.isEmpty()) {
                itemsProcesados.add(crearItemMapa(1L, "Whey Protein 2 LB", "Suplementos", "/img/whey.jpg", 149.00, 1));
            }
        }

        int numPedido;
        if ("PAGADO".equals(estadoPago) || "EN_PROCESO".equals(estadoPago)) {
            if (primeraCompraDemoReemplazada.compareAndSet(false, true)) {
                numPedido = 521;
            } else {
                numPedido = secuenciaPedido.incrementAndGet();
            }
            if (reservaPago == null) {
                descontarStockDeProductos(itemsProcesados);
            }
        } else {
            numPedido = primeraCompraDemoReemplazada.get() ? secuenciaPedido.get() + 1 : 521;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", new Locale("es", "PE"));
        String fechaActual = LocalDateTime.now().format(fmt);

        String estadoEntrega = "PAGADO".equals(estadoPago)
                ? "POR RECOGER EN GIMNASIO"
                : ("EN_PROCESO".equals(estadoPago) ? "EN VALIDACIÓN BANCARIA" : "SIN CARGO");

        Map<String, Object> nuevaOrden = construirOrdenMapa(
                numPedido, fechaActual, estadoPago, estadoEntrega,
                buyerName, buyerDocType, buyerDocNum, buyerEmail,
                cardBrand, cardLast4, itemsProcesados
        );
        if (reservaId != null) {
            nuevaOrden.put("reservaId", reservaId);
        }

        String codigo = (String) nuevaOrden.get("codigo");
        ordenesPorCodigo.put(codigo, nuevaOrden);

        if ("PAGADO".equals(estadoPago) || "EN_PROCESO".equals(estadoPago)) {
            registrarOrdenEnMemoria(nuevaOrden, true);
        }
        if ("PAGADO".equals(estadoPago) && reservaPago != null) {
            reservaService.actualizarEstado(reservaId, EstadoReserva.CONFIRMADA);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "codigo", codigo,
                "boleta", nuevaOrden.get("boleta"),
                "orden", nuevaOrden
        ));
    }

    @PostMapping("/api/compras/aprobar")
    @ResponseBody
    public ResponseEntity<?> aprobarCompraEnProceso(@RequestParam(defaultValue = "#PED-00521") String codigo,
                                                    Authentication auth) {
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        if (orden != null) {
            Object reservaId = orden.get("reservaId");
            if (reservaId != null) {
                Long id = ((Number) reservaId).longValue();
                Reserva reserva = reservaService.buscarPorId(id).orElse(null);
                if (reserva == null || auth == null
                        || !auth.getName().equals(reserva.getSocio().getUsuario().getUsername())) {
                    return ResponseEntity.notFound().build();
                }
                if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Esta reserva ya no está pendiente de pago."));
                }
                reservaService.actualizarEstado(id, EstadoReserva.CONFIRMADA);
            }
            orden.put("estadoPago", "PAGADO");
            orden.put("estadoEntrega", "POR RECOGER EN GIMNASIO");
            registrarOrdenEnMemoria(orden, true);
        }
        return ResponseEntity.ok(Map.of("success", true, "codigo", codigo));
    }

    @GetMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> listarComprasEnMemoria() {
        return ResponseEntity.ok(ordenesMemoria);
    }

    // 1. Tu carrito
    @GetMapping("/carrito")
    public String verCarrito(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/carrito";
    }

    // 2. Pago en línea
    @GetMapping("/pago-en-linea")
    public String pagoEnLinea(@RequestParam(required = false) Long reservaId,
                              Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        if (reservaId != null) {
            Reserva reserva = reservaService.buscarPorId(reservaId)
                    .filter(r -> auth.getName().equals(r.getSocio().getUsuario().getUsername()))
                    .orElse(null);
            if (reserva == null || reserva.getEstado() != EstadoReserva.PENDIENTE) {
                model.addAttribute("reservaPagoError", "La reserva no está disponible para pago. Revisa su estado en Mis Reservas.");
            } else {
                model.addAttribute("reservaPago", reserva);
            }
        }
        return "cliente/pago-en-linea";
    }

    // 3. Pago en proceso
    @GetMapping("/pago-en-proceso")
    public String pagoEnProceso(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/pago-en-proceso";
    }

    // 4. Pago rechazado
    @GetMapping("/pago-rechazado")
    public String pagoRechazado(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/pago-rechazado";
    }

    // 5. Pago cancelado
    @GetMapping("/pago-cancelado")
    public String pagoCancelado(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/pago-cancelado";
    }

    // 6. Compra exitosa
    @GetMapping("/compra-exitosa")
    public String compraExitosa(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/compra-exitosa";
    }

    // 7. Detalle del pedido
    @GetMapping("/detalle-pedido")
    public String detallePedido(@RequestParam(required = false) String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/detalle-pedido";
    }

    // 8. Mis compras
    @GetMapping("/mis-compras")
    public String misCompras(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/mis-compras";
    }

    // 9. Boleta de venta
    @GetMapping("/boleta-venta")
    public String boletaVenta(@RequestParam(required = false) String codigo,
                              Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        Map<String, Object> orden = buscarOrdenPorCodigoODefault(codigo);
        model.addAttribute("ordenActual", orden);
        model.addAttribute("codigoPedido", orden.get("codigo"));
        return "cliente/boleta-venta";
    }
}
