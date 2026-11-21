package pe.edu.utp.Grupo04.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import pe.edu.utp.Grupo04.dto.CompraRequestDTO;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.exception.StockInsuficienteException;
import pe.edu.utp.Grupo04.service.OrdenCompraService;
import pe.edu.utp.Grupo04.util.BoletaTxt;
import pe.edu.utp.Grupo04.validation.CompraValidator;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuestas HTTP de la API de compras simuladas. Compartido por los controllers
 * público y de cliente para no duplicar código.
 */
@Slf4j
@Component
public class CompraApiResponder {

    private final OrdenCompraService ordenCompraService;
    private final CompraValidator compraValidator;

    public CompraApiResponder(OrdenCompraService ordenCompraService, CompraValidator compraValidator) {
        this.ordenCompraService = ordenCompraService;
        this.compraValidator = compraValidator;
    }

    /** La identidad SIEMPRE viene de la sesión, nunca del cuerpo de la solicitud. */
    public static String username(Authentication auth) {
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : null;
    }

    private ResponseEntity<Map<String, Object>> error(int status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }

    public ResponseEntity<?> procesarCompra(CompraRequestDTO request, BindingResult br, Authentication auth) {
        String username = username(auth);
        if (username == null) {
            return error(401, "Debes iniciar sesión para comprar");
        }

        compraValidator.validate(request, br);
        if (br.hasErrors()) {
            Map<String, String> errores = new LinkedHashMap<>();
            for (FieldError fe : br.getFieldErrors()) {
                errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("mensaje", "Datos de compra incompletos o inválidos");
            body.put("errores", errores);
            return ResponseEntity.badRequest().body(body);
        }

        try {
            Map<String, Object> orden = ordenCompraService.registrarCompra(request, username);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("codigo", orden.get("codigo"));
            body.put("boleta", orden.get("boleta"));
            body.put("estadoPago", orden.get("estadoPago"));
            body.put("orden", orden);
            return ResponseEntity.ok(body);
        } catch (StockInsuficienteException e) {
            return error(409, e.getMessage());
        } catch (RecursoNoEncontradoException e) {
            return error(422, e.getMessage());
        } catch (AccessDeniedException e) {
            return error(403, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return error(409, "Esta operación ya se está procesando. Intenta de nuevo.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(400, e.getMessage());
        }
    }

    /** aprobar = true aprueba la operación pendiente; false la cancela. */
    public ResponseEntity<?> resolverPendiente(boolean aprobar, String codigo, Authentication auth) {
        String username = username(auth);
        if (username == null) {
            return error(401, "Debes iniciar sesión");
        }
        try {
            Map<String, Object> orden = aprobar
                    ? ordenCompraService.aprobarCompra(codigo, username)
                    : ordenCompraService.cancelarCompra(codigo, username);
            String estado = String.valueOf(orden.get("estadoPago"));
            boolean ok = aprobar ? "PAGADO".equals(estado) : "CANCELADO".equals(estado);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", ok);
            body.put("codigo", orden.get("codigo"));
            body.put("estadoPago", estado);
            if (!ok) {
                body.put("mensaje", "La operación ya no está pendiente (estado: " + estado + ")");
            }
            return ResponseEntity.status(ok ? 200 : 409).body(body);
        } catch (RecursoNoEncontradoException e) {
            return error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return error(409, e.getMessage());
        }
    }

    public ResponseEntity<byte[]> descargarBoleta(String codigo, Authentication auth) {
        String username = username(auth);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        return ordenCompraService.buscarPorCodigoYPropietario(codigo, username)
                .filter(o -> "PAGADO".equals(o.get("estadoPago")))
                .map(CompraApiResponder::boleta)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    static ResponseEntity<byte[]> boleta(Map<String, Object> orden) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"boleta_" + orden.get("boleta") + ".txt\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(BoletaTxt.construir(orden).getBytes(StandardCharsets.UTF_8));
    }
}
