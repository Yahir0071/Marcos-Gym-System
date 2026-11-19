package pe.edu.utp.Grupo04.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filtro de verificación activa de sesión HTTP:
 * Valida en cada petición (incluyendo APIs que usan sesión) que el usuario autenticado por cookies:
 * 1. Continúe existiendo y activo en la base de datos (Feedback #4).
 * 2. Su rol en la sesión coincida con el rol actual en la BD (Feedback #4).
 * Si la cuenta fue desactivada o cambió de rol, la sesión se invalida de inmediato:
 * - Para APIs (AJAX/JSON/REST): responde con HTTP 401 Unauthorized JSON.
 * - Para navegación web normal: redirige a /login.
 */
@Component
public class SessionValidityFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public SessionValidityFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Solo verificar si hay usuario autenticado en sesión web (no JWT)
        String authHeader = request.getHeader("Authorization");
        boolean esPeticionJwt = authHeader != null && authHeader.startsWith("Bearer ");

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()) && !esPeticionJwt) {
            String username = auth.getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            boolean esInactivo = usuario == null || !Boolean.TRUE.equals(usuario.getActivo());
            boolean tieneRolValido = !esInactivo && auth.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_" + usuario.getRol().name()));

            if (esInactivo || !tieneRolValido) {
                // Invalidar sesión y limpiar contexto de seguridad
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }

                String uri = request.getRequestURI();
                String acceptHeader = request.getHeader("Accept");
                boolean esApi = uri.startsWith("/api/") || uri.contains("/api/") ||
                        (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

                if (esApi) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    String mensaje = esInactivo ? "La cuenta ha sido desactivada." : "El rol del usuario ha sido modificado. Sesión revocada.";
                    response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"mensaje\":\"" + mensaje + "\"}");
                    return;
                } else {
                    String redirectUrl = esInactivo ? "/login?error=inactivo" : "/login?error=rol_actualizado";
                    response.sendRedirect(redirectUrl);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
