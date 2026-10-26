package pe.edu.utp.Grupo04.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.SocioService;

@Controller
@RequestMapping("/cliente")
public class CompraClienteController {

    private final SocioService socioService;
    private final ProductoService productoService;

    public CompraClienteController(SocioService socioService, ProductoService productoService) {
        this.socioService = socioService;
        this.productoService = productoService;
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
    }

    // 1. Tu carrito
    @GetMapping("/carrito")
    public String verCarrito(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/carrito";
    }

    // 2. Pago en línea
    @GetMapping("/pago-en-linea")
    public String pagoEnLinea(Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        return "cliente/pago-en-linea";
    }

    // 3. Pago en proceso
    @GetMapping("/pago-en-proceso")
    public String pagoEnProceso(@RequestParam(defaultValue = "#PED-00521") String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("codigoPedido", codigo);
        return "cliente/pago-en-proceso";
    }

    // 4. Pago rechazado
    @GetMapping("/pago-rechazado")
    public String pagoRechazado(@RequestParam(defaultValue = "#PED-00521") String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("codigoPedido", codigo);
        return "cliente/pago-rechazado";
    }

    // 5. Pago cancelado
    @GetMapping("/pago-cancelado")
    public String pagoCancelado(@RequestParam(defaultValue = "#PED-00521") String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("codigoPedido", codigo);
        return "cliente/pago-cancelado";
    }

    // 6. Compra exitosa
    @GetMapping("/compra-exitosa")
    public String compraExitosa(@RequestParam(defaultValue = "#PED-00521") String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("codigoPedido", codigo);
        return "cliente/compra-exitosa";
    }

    // 7. Detalle del pedido
    @GetMapping("/detalle-pedido")
    public String detallePedido(@RequestParam(defaultValue = "#PED-00521") String codigo,
                                Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("codigoPedido", codigo);
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
    public String boletaVenta(@RequestParam(defaultValue = "#PED-00521") String codigo,
                              Authentication auth, Model model) {
        cargarDatosSocio(auth, model);
        model.addAttribute("codigoPedido", codigo);
        return "cliente/boleta-venta";
    }
}
