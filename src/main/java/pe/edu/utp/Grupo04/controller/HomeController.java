package pe.edu.utp.Grupo04.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.CompraItemDTO;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.exception.ValidacionNegocioException;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final TransactionTemplate transacciones;

    public HomeController(PlanService planService,
                          PromocionService promocionService,
                          ProductoService productoService,
                          SocioService socioService,
                          MembresiaService membresiaService,
                          OrdenCompraService ordenCompraService,
                          TransactionTemplate transacciones) {
        this.planService = planService;
        this.promocionService = promocionService;
        this.productoService = productoService;
        this.socioService = socioService;
        this.membresiaService = membresiaService;
        this.ordenCompraService = ordenCompraService;
        this.transacciones = transacciones;
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

    private static final Set<String> PERIODOS = Set.of("MENSUAL", "TRIMESTRAL", "ANUAL");
    private static final Set<String> GENEROS = Set.of("MASCULINO", "FEMENINO", "OTRO", "PREFIERO_NO_DECIR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @GetMapping({"/registrarse", "/register"})
    public String vistaRegistro(@RequestParam(required = false) Long planId,
                                @RequestParam(defaultValue = "MENSUAL") String periodo,
                                Model model) {
        var planes = planService.listarActivos();
        Plan elegido = planes.stream().filter(p -> p.getId().equals(planId)).findFirst().orElseGet(() ->
                planes.stream().filter(p -> p.getNombre().toUpperCase(Locale.ROOT).contains("PLUS"))
                        .findFirst().orElseGet(() -> planes.isEmpty() ? null : planes.getFirst()));
        prepararResumen(model, elegido != null && elegido.getPrecio() != null && elegido.getPrecio() >= 1 ? elegido : null,
                PERIODOS.contains(periodo) ? periodo : "MENSUAL", "");
        return "register";
    }

    @PostMapping({"/registrarse", "/registro"})
    public String procesarRegistro(@ModelAttribute RegistroForm datos, Model model, RedirectAttributes flash) {
        var planes = planService.listarActivos();
        Plan elegido = planes.stream().filter(p -> p.getId().toString().equals(datos.planId()))
                .findFirst().orElse(null);
        String periodo = texto(datos.periodo());
        String metodo = texto(datos.metodoPago());
        String comprobante = texto(datos.tipoComprobante());
        String cupon = texto(datos.cupon()).toUpperCase(Locale.ROOT);
        boolean cuponValido = cupon.isEmpty() || TarifaInscripcion.CUPONES_DEMO.contains(cupon);
        boolean planValido = elegido != null && elegido.getPrecio() != null && elegido.getPrecio() >= 1;
        prepararResumen(model, planValido ? elegido : null,
                PERIODOS.contains(periodo) ? periodo : "MENSUAL", cuponValido ? cupon : "");
        preservarCampos(model, datos, metodo, comprobante);

        if (!planValido || !PERIODOS.contains(periodo)) {
            return volverAlRegistro(model, 1, "Selecciona un plan disponible y una duración válida.");
        }
        if (!Set.of("ONLINE", "RECEPCION").contains(metodo) || !cuponValido) {
            return volverAlRegistro(model, 3, "Revisa la forma de pago y el cupón aplicado.");
        }
        String nombres = texto(datos.nombres()), apellidos = texto(datos.apellidos()), dni = texto(datos.dni());
        String telefono = texto(datos.telefono()), genero = texto(datos.genero()), email = texto(datos.email());
        if (nombres.isEmpty() || apellidos.isEmpty() || nombres.length() > 100 || apellidos.length() > 100) {
            return volverAlRegistro(model, 1, "Ingresa nombres y apellidos de hasta 100 caracteres cada uno.");
        }
        if (!dni.matches("[0-9]{8}") || (!telefono.isEmpty() && !telefono.matches("[0-9]{9}"))) {
            return volverAlRegistro(model, 1, "El DNI debe tener 8 dígitos y el teléfono 9, si lo ingresas.");
        }
        if (!genero.isEmpty() && !GENEROS.contains(genero)) {
            return volverAlRegistro(model, 1, "Selecciona una opción de género válida.");
        }

        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        LocalDate nacimiento = null;
        if (!texto(datos.fechaNacimiento()).isEmpty()) {
            try {
                nacimiento = LocalDate.parse(datos.fechaNacimiento());
                if (nacimiento.isAfter(hoy) || nacimiento.isBefore(hoy.minusYears(120))) {
                    return volverAlRegistro(model, 1, "Ingresa una fecha de nacimiento válida.");
                }
            } catch (DateTimeParseException e) {
                return volverAlRegistro(model, 1, "Ingresa una fecha de nacimiento válida.");
            }
        }

        String password = datos.password();
        if (email.length() > 120 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            return volverAlRegistro(model, 2, "Ingresa un correo válido de hasta 120 caracteres.");
        }
        if (password == null || password.strip().length() < 6 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return volverAlRegistro(model, 2, "Usa una contraseña de al menos 6 caracteres y hasta 72 bytes.");
        }
        if (!password.equals(datos.confirmPassword())) {
            return volverAlRegistro(model, 2, "Confirma la contraseña. Ambos campos deben coincidir.");
        }
        String ruc = texto(datos.rucFactura()), razonSocial = texto(datos.razonSocialFactura());
        if (!Set.of("BOLETA", "FACTURA").contains(comprobante)
                || ("FACTURA".equals(comprobante) && (!ruc.matches("[0-9]{11}") || razonSocial.isEmpty() || razonSocial.length() > 120))) {
            return volverAlRegistro(model, 2, "Para elegir factura, ingresa un RUC de 11 dígitos y una razón social de hasta 120 caracteres.");
        }
        if (!"true".equals(datos.aceptaTerminos())) {
            return volverAlRegistro(model, 4, "Confirma que aceptas las condiciones para completar la inscripción.");
        }

        Socio nuevo = new Socio();
        nuevo.setNombres(nombres);
        nuevo.setApellidos(apellidos);
        nuevo.setDni(dni);
        nuevo.setTelefono(telefono.isEmpty() ? null : telefono);
        nuevo.setGenero(genero.isEmpty() ? null : genero);
        nuevo.setFechaNacimiento(nacimiento);
        TarifaInscripcion tarifa = TarifaInscripcion.calcular(elegido, periodo, cupon);
        try {
            if ("RECEPCION".equals(metodo)) {
                var reserva = transacciones.execute(status -> {
                    Socio socio = socioService.registrarSocio(nuevo, email, password);
                    return membresiaService.crearPreinscripcion(socio, elegido, tarifa);
                });
                flash.addFlashAttribute("mensajeExito", "Tu preinscripción quedó registrada. La tarifa se conserva por 72 horas.");
                flash.addFlashAttribute("codigoInscripcion", reserva.getCodigoPreinscripcion());
                flash.addFlashAttribute("fechaLimite", reserva.getPreinscripcionVence().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                flash.addFlashAttribute("montoPagar", reserva.getMontoReservado());
                flash.addFlashAttribute("planNombre", elegido.getNombre());
                return "redirect:/login?preinscrito=true";
            }

            CompraRequestDTO compra = new CompraRequestDTO();
            compra.setTipoConcepto("MEMBRESIA");
            compra.setPlanId(elegido.getId());
            compra.setPeriodo(periodo);
            compra.setBuyerName("FACTURA".equals(comprobante) ? razonSocial : nombres + " " + apellidos);
            compra.setBuyerEmail(email);
            compra.setBuyerDocType("FACTURA".equals(comprobante) ? "RUC" : "DNI");
            compra.setBuyerDocNum("FACTURA".equals(comprobante) ? ruc : dni);
            compra.setCardBrand("SIMULADO");
            compra.setEstado("PAGADO");

            CompraItemDTO item = new CompraItemDTO();
            item.setId(elegido.getId());
            item.setName("MEMBRESÍA " + elegido.getNombre() + " (" + periodo + ")");
            item.setCategory("MEMBRESIA");
            item.setPrice(tarifa.total().doubleValue());
            item.setQty(1);
            compra.getItems().add(item);

            Map<String, Object> orden = transacciones.execute(status -> {
                Socio socio = socioService.registrarSocio(nuevo, email, password);
                membresiaService.asignarPlanASocio(socio.getId(), elegido.getId(), hoy, periodo, tarifa.meses());
                return ordenCompraService.registrarCompraDeInscripcion(compra, socio.getUsuario().getUsername(),
                        tarifa.total().doubleValue());
            });
            flash.addFlashAttribute("boleta", orden.get("boleta"));
            flash.addFlashAttribute("planNombre", elegido.getNombre());
            return "redirect:/login?activado=true";
        } catch (ValidacionNegocioException e) {
            return volverAlRegistro(model, 1, e.getMessage());
        } catch (IllegalArgumentException | DataIntegrityViolationException e) {
            return volverAlRegistro(model, 1, "Verifica que el DNI y el correo no estén registrados y que el plan siga disponible.");
        } catch (Exception e) {
            log.error("No se pudo completar la inscripción", e);
            return volverAlRegistro(model, 2, "No se pudo completar la inscripción. Revisa tus datos e inténtalo de nuevo.");
        }
    }

    private void prepararResumen(Model model, Plan plan, String periodo, String cupon) {
        model.addAttribute("selectedPlan", plan);
        model.addAttribute("planId", plan == null ? null : plan.getId());
        model.addAttribute("cupon", cupon);
        model.addAttribute("periodo", periodo);
        model.addAttribute("fechaNacimientoMax", LocalDate.now(TarifaInscripcion.ZONA_LIMA));
        model.addAttribute("cuponesValidos", TarifaInscripcion.CUPONES_DEMO);
        if (plan == null || plan.getPrecio() == null || plan.getPrecio() < 1) {
            return;
        }
        TarifaInscripcion tarifa = TarifaInscripcion.calcular(plan, periodo, cupon);
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        int dias = plan.getDuracionDias() != null && plan.getDuracionDias() > 0 ? plan.getDuracionDias() : 30;
        model.addAttribute("meses", tarifa.meses());
        model.addAttribute("baseMensual", plan.getPrecio());
        model.addAttribute("diasPlanMensual", dias);
        model.addAttribute("precioTotal", tarifa.total());
        model.addAttribute("subtotalSinDescuento", tarifa.subtotal());
        model.addAttribute("discPct", tarifa.porcentajePeriodo());
        model.addAttribute("descuentoCupon", tarifa.descuentoCupon());
        model.addAttribute("ahorroEstimado", tarifa.subtotal().subtract(tarifa.total()).subtract(tarifa.descuentoCupon()));
        model.addAttribute("fechaInicioIso", hoy.toString());
        model.addAttribute("fechaInicio", hoy.format(FECHA));
        model.addAttribute("fechaFinEstimada", tarifa.fechaFin(hoy, dias).format(FECHA));
    }

    private void preservarCampos(Model model, RegistroForm datos, String metodo, String comprobante) {
        model.addAttribute("nombres", datos.nombres());
        model.addAttribute("apellidos", datos.apellidos());
        model.addAttribute("dni", datos.dni());
        model.addAttribute("telefono", datos.telefono());
        model.addAttribute("genero", datos.genero());
        model.addAttribute("fechaNacimiento", datos.fechaNacimiento());
        model.addAttribute("email", datos.email());
        model.addAttribute("tipoComprobante", comprobante);
        model.addAttribute("rucFactura", datos.rucFactura());
        model.addAttribute("razonSocialFactura", datos.razonSocialFactura());
        model.addAttribute("metodoPago", metodo);
        model.addAttribute("aceptaTerminos", "true".equals(datos.aceptaTerminos()));
    }

    private String volverAlRegistro(Model model, int paso, String mensaje) {
        model.addAttribute("pasoError", paso);
        model.addAttribute("mensajeError", mensaje);
        return "register";
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.strip();
    }

    public record RegistroForm(String nombres, String apellidos, String dni, String telefono, String genero,
                               String fechaNacimiento, String email, String password, String confirmPassword,
                               String planId, String periodo, String metodoPago, String tipoComprobante,
                               String rucFactura, String razonSocialFactura, String cupon, String aceptaTerminos) {
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout=true";
    }
}
