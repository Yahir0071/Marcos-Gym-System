package pe.edu.utp.Grupo04.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.exception.AccesoDenegadoException;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.exception.ValidacionNegocioException;
import pe.edu.utp.Grupo04.security.ApiSecurityError;

import java.util.HashMap;
import java.util.Map;

/**
 * M02: Manejador global centralizado de excepciones y validaciones.
 * Evita respuestas genéricas HTTP 500 y devuelve mensajes comprensibles en español.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean esPeticionApi(HttpServletRequest request) {
        return ApiSecurityError.esApi(request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Error de validación @Valid en {}: {}", request.getRequestURI(), errores);

        if (esPeticionApi(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", "Datos de entrada inválidos o incompletos",
                    "errores", errores
            ));
        }

        String primerError = errores.values().stream().findFirst().orElse("Datos inválidos en el formulario.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", primerError));
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException ex, HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Error de binding en {}: {}", request.getRequestURI(), errores);

        if (esPeticionApi(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", "Datos de formulario inválidos",
                    "errores", errores
            ));
        }

        String primerError = errores.values().stream().findFirst().orElse("Datos de formulario inválidos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", primerError));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Violación de restricción en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "mensaje", "Restricción de validación no cumplida",
                "detalle", ex.getMessage()
        ));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public Object handleRecursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado (404) en {}: {}", request.getRequestURI(), ex.getMessage());
        if (esPeticionApi(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "mensaje", ex.getMessage()
            ));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public Object handleAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest request) {
        log.warn("Acceso denegado (403) en {}: {}", request.getRequestURI(), ex.getMessage());
        if (esPeticionApi(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "error", ApiSecurityError.FORBIDDEN.error(),
                    "mensaje", ex.getMessage()
            ));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public Object handleValidacionNegocio(ValidacionNegocioException ex, HttpServletRequest request) {
        log.warn("Error de regla de negocio en {}: {}", request.getRequestURI(), ex.getMessage());
        if (esPeticionApi(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", ex.getMessage()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento ilegal en {}: {}", request.getRequestURI(), ex.getMessage());
        if (esPeticionApi(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", ex.getMessage()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Estado ilegal en {}: {}", request.getRequestURI(), ex.getMessage());
        if (esPeticionApi(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "mensaje", ex.getMessage()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
