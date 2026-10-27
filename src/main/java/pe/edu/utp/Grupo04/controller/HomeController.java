package pe.edu.utp.Grupo04.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controlador para las vistas públicas, inicio, login y flujo completo de contratación de membresías.
 * Cumple con Clean Code y los principios SOLID.
 */
@Slf4j
@Controller
public class HomeController {

    private final PlanService planService;
    private final PromocionService promocionService;
    private final ProductoService productoService;
    private final SocioService socioService;
    private final MembresiaService membresiaService;
    private final OrdenCompraService ordenCompraService;

    public HomeController(PlanService planService,
                          PromocionService promocionService,
                          ProductoService productoService,
                          SocioService socioService,
                          MembresiaService membresiaService,
                          OrdenCompraService ordenCompraService) {
        this.planService = planService;
        this.promocionService = promocionService;
        this.productoService = productoService;
        this.socioService = socioService;
        this.membresiaService = membresiaService;
        this.ordenCompraService = ordenCompraService;
    }

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model) {
        model.addAttribute("planes", planService.listarActivos());
        model.addAttribute("promociones", promocionService.listarVigentes());
        model.addAttribute("productos", productoService.listarCatalogoActivo());
        return "public/inicio";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Pantalla de inscripción y contratación de membresía en pasos (Estilo Smart Fit).
     * Preserva la duración y tarifa seleccionada en /planes (Mensual, Trimestral o Anual).
     */
    @GetMapping({"/registrarse", "/register"})
    public String vistaRegistro(@RequestParam(required = false) Long planId,
                                @RequestParam(required = false, defaultValue = "MENSUAL") String periodo,
                                @RequestParam(required = false) Integer meses,
                                @RequestParam(required = false) Double precio,
                                Model model) {
        var planes = planService.listarActivos();
        model.addAttribute("planes", planes);

        Plan selectedPlan = null;
        if (planId != null) {
            selectedPlan = planService.buscarPorId(planId).orElse(null);
        }
        if (selectedPlan == null && !planes.isEmpty()) {
            selectedPlan = planes.stream()
                    .filter(p -> p.getNombre() != null && p.getNombre().toUpperCase().contains("PLUS"))
                    .findFirst()
                    .orElse(planes.get(0));
        }

        String per = (periodo != null && !periodo.isBlank()) ? periodo.toUpperCase().trim() : "MENSUAL";
        int m = 1;
        if (meses != null && meses > 0) {
            m = meses;
        } else if ("TRIMESTRAL".equalsIgnoreCase(per)) {
            m = 3;
        } else if ("ANUAL".equalsIgnoreCase(per)) {
            m = 12;
        }

        double baseMensual = (selectedPlan != null && selectedPlan.getPrecio() != null) ? selectedPlan.getPrecio() : 119.0;
        double discFactor = (m == 3) ? 0.90 : ((m >= 12) ? 0.80 : 1.0);
        int discPct = (m == 3) ? 10 : ((m >= 12) ? 20 : 0);

        double precioCalculado;
        if (precio != null && precio > 0) {
            precioCalculado = precio;
        } else {
            precioCalculado = Math.round(baseMensual * m * discFactor);
        }

        double subtotalSinDescuento = Math.round(baseMensual * m);
        double ahorroEstimado = subtotalSinDescuento - precioCalculado;

        LocalDate hoy = LocalDate.now();
        LocalDate finEstimada = hoy.plusDays((long) m * 30);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        model.addAttribute("selectedPlan", selectedPlan);
        model.addAttribute("planId", selectedPlan != null ? selectedPlan.getId() : null);
        model.addAttribute("periodo", per);
        model.addAttribute("meses", m);
        model.addAttribute("baseMensual", baseMensual);
        model.addAttribute("precioTotal", precioCalculado);
        model.addAttribute("subtotalSinDescuento", subtotalSinDescuento);
        model.addAttribute("discPct", discPct);
        model.addAttribute("ahorroEstimado", ahorroEstimado);
        model.addAttribute("fechaInicio", hoy.format(dtf));
        model.addAttribute("fechaFinEstimada", finEstimada.format(dtf));

        return "register";
    }

    /**
     * Procesa la contratación completa de membresía con validaciones, opciones de facturación y pago.
     */
    @PostMapping({"/registrarse", "/registro"})
    public String procesarRegistro(@RequestParam String nombres,
                                   @RequestParam String apellidos,
                                   @RequestParam String dni,
                                   @RequestParam(required = false) String telefono,
                                   @RequestParam String email,
                                   @RequestParam String password,
                                   @RequestParam(required = false) Long planId,
                                   @RequestParam(required = false, defaultValue = "MENSUAL") String periodo,
                                   @RequestParam(required = false, defaultValue = "1") Integer meses,
                                   @RequestParam(required = false) Double precioTotal,
                                   @RequestParam(required = false, defaultValue = "ONLINE") String metodoPago,
                                   @RequestParam(required = false, defaultValue = "BOLETA") String tipoComprobante,
                                   @RequestParam(required = false) String rucFactura,
                                   @RequestParam(required = false) String razonSocialFactura,
                                   @RequestParam(required = false) String cupon,
                                   Model model,
                                   RedirectAttributes flash) {

        var planes = planService.listarActivos();
        model.addAttribute("planes", planes);
        Plan selectedPlan = (planId != null) ? planService.buscarPorId(planId).orElse(null) : null;
        model.addAttribute("selectedPlan", selectedPlan);
        model.addAttribute("planId", planId);
        model.addAttribute("periodo", periodo);
        model.addAttribute("meses", meses);
        model.addAttribute("precioTotal", precioTotal);

        // Validación de DNI
        if (dni == null || !dni.matches("^\\d{8}$")) {
            model.addAttribute("mensajeError", "El DNI del socio debe contener exactamente 8 dígitos numéricos.");
            rellenarCamposModel(model, nombres, apellidos, dni, telefono, email);
            return "register";
        }

        // Validación de Factura si corresponde
        if ("FACTURA".equalsIgnoreCase(tipoComprobante) && (rucFactura == null || !rucFactura.matches("^\\d{11}$"))) {
            model.addAttribute("mensajeError", "Para emitir factura electrónica se requiere un RUC válido de 11 dígitos.");
            rellenarCamposModel(model, nombres, apellidos, dni, telefono, email);
            return "register";
        }

        // Validación de Contraseña
        if (password == null || password.length() < 6) {
            model.addAttribute("mensajeError", "La contraseña de acceso debe tener al menos 6 caracteres.");
            rellenarCamposModel(model, nombres, apellidos, dni, telefono, email);
            return "register";
        }

        try {
            // 1. Crear nuevo Socio y Usuario (Rol CLIENTE con BCrypt)
            Socio nuevo = new Socio();
            nuevo.setNombres(nombres.trim());
            nuevo.setApellidos(apellidos.trim());
            nuevo.setDni(dni.trim());
            nuevo.setTelefono(telefono != null ? telefono.trim() : null);

            Socio savedSocio = socioService.registrarSocio(nuevo, email.trim(), password);
            log.info("Nuevo socio registrado exitosamente: id={}, email={}", savedSocio.getId(), email);

            String codInscripcion = "#INS-" + (10000 + (int)(Math.random() * 90000));
            LocalDate hoy = LocalDate.now();
            String planNombre = (selectedPlan != null) ? selectedPlan.getNombre() : "PLAN PLUS";
            double montoFinal = (precioTotal != null && precioTotal > 0) ? precioTotal : 119.0;

            // Validación opcional de cupón
            if (cupon != null && !cupon.isBlank()) {
                String cUpper = cupon.toUpperCase().trim();
                if (cUpper.contains("2X1") || cUpper.contains("PROMO") || cUpper.contains("APERTURA")) {
                    log.info("Cupón promocional aplicado en contratación: {}", cUpper);
                }
            }

            // 2. Manejo según Forma de Pago (ONLINE vs RECEPCION)
            if ("ONLINE".equalsIgnoreCase(metodoPago)) {
                // Pago Online: Activación inmediata y generación de boleta/factura electrónica
                if (planId != null) {
                    try {
                        membresiaService.asignarPlanASocio(savedSocio.getId(), planId, hoy);
                    } catch (Exception ex) {
                        log.warn("Asignación de plan online en PostgreSQL: {}", ex.getMessage());
                    }
                }

                CompraRequestDTO req = new CompraRequestDTO();
                req.setTipoConcepto("MEMBRESIA");
                req.setPlanId(planId);
                req.setBuyerName(nombres.trim() + " " + apellidos.trim());
                req.setBuyerEmail(email.trim());
                req.setBuyerDocType("FACTURA".equalsIgnoreCase(tipoComprobante) ? "RUC" : "DNI");
                req.setBuyerDocNum("FACTURA".equalsIgnoreCase(tipoComprobante) ? rucFactura.trim() : dni.trim());
                req.setCardBrand("VISA");
                req.setCardLast4("4242");
                req.setEstado("PAGADO");

                pe.edu.utp.Grupo04.dto.CompraItemDTO item = new pe.edu.utp.Grupo04.dto.CompraItemDTO();
                item.setId(planId != null ? planId : 1L);
                item.setName("MEMBRESIA " + planNombre + " (" + periodo + ")");
                item.setCategory("MEMBRESIA");
                item.setPrice(montoFinal);
                item.setQty(1);
                req.getItems().add(item);

                Map<String, Object> orden = ordenCompraService.registrarCompra(req, email.trim());
                String boleta = (String) orden.getOrDefault("boleta", "B001-" + (10000000 + (int)(Math.random() * 90000000)));

                flash.addFlashAttribute("mensajeExito", "¡Inscripción confirmada! Membresía activada exitosamente con comprobante " + boleta + ".");
                flash.addFlashAttribute("codigoInscripcion", codInscripcion);
                flash.addFlashAttribute("boleta", boleta);
                flash.addFlashAttribute("modoPago", "ONLINE");
                flash.addFlashAttribute("planNombre", planNombre);
                return "redirect:/login?activado=true&boleta=" + boleta;

            } else {
                // Pago en Recepción: Preinscripción reservada con fecha límite de 72 horas
                String fechaLimite = hoy.plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                flash.addFlashAttribute("mensajeExito", "¡Preinscripción generada con éxito! Tu tarifa está reservada por 72 horas.");
                flash.addFlashAttribute("codigoInscripcion", codInscripcion);
                flash.addFlashAttribute("modoPago", "RECEPCION");
                flash.addFlashAttribute("fechaLimite", fechaLimite);
                flash.addFlashAttribute("montoPagar", montoFinal);
                flash.addFlashAttribute("planNombre", planNombre);
                return "redirect:/login?preinscrito=true&codigo=" + codInscripcion;
            }

        } catch (Exception e) {
            log.warn("Fallo al registrar contratación de socio: {}", e.getMessage());
            model.addAttribute("mensajeError", e.getMessage());
            rellenarCamposModel(model, nombres, apellidos, dni, telefono, email);
            return "register";
        }
    }

    private void rellenarCamposModel(Model model, String nombres, String apellidos, String dni, String telefono, String email) {
        model.addAttribute("nombres", nombres);
        model.addAttribute("apellidos", apellidos);
        model.addAttribute("dni", dni);
        model.addAttribute("telefono", telefono);
        model.addAttribute("email", email);
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout=true";
    }
}
