package pe.edu.utp.Grupo04.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record ApiSecurityError(String error, String mensaje) {
    public static final ApiSecurityError UNAUTHORIZED =
            new ApiSecurityError("UNAUTHORIZED", "Inicia sesión para continuar.");
    public static final ApiSecurityError FORBIDDEN =
            new ApiSecurityError("FORBIDDEN", "No tienes permiso para realizar esta acción.");
    public static final ApiSecurityError CREDENTIALS =
            new ApiSecurityError("UNAUTHORIZED", "El usuario o la contraseña son incorrectos.");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static String ruta(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public static boolean esApi(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return ruta(request).contains("/api/") || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    public void responder(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JSON.writeValue(response.getWriter(), this);
    }
}
