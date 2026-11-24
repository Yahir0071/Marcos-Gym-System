package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;

/** Rutas de compra desde el catálogo público. Exigen sesión iniciada para comprar o descargar. */
@Controller
public class CompraPublicaController {

    private final CompraApiResponder responder;

    public CompraPublicaController(CompraApiResponder responder) {
        this.responder = responder;
    }

    @PostMapping("/api/compras")
    @ResponseBody
    public ResponseEntity<?> registrar(@Valid @RequestBody CompraRequestDTO request,
                                       BindingResult bindingResult,
                                       Authentication auth) {
        return responder.procesarCompra(request, bindingResult, auth);
    }

    @GetMapping("/api/boleta-venta/descargar")
    public ResponseEntity<byte[]> descargar(@RequestParam(required = false) String codigo,
                                            Authentication auth) {
        return responder.descargarBoleta(codigo, auth);
    }
}