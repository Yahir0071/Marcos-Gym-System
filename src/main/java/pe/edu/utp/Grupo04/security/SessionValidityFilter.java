package pe.edu.utp.Grupo04.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro de verificación activa de sesión HTTP:
 * Valida en cada petición web que el usuario autenticado por sesión de cookies:
 * 1. Continúe existiendo y activo en la base de datos (Feedback #4).
 * 2. Su rol en la sesión coincida con el rol actual en la BD (Feedback #4).
 * Si la cuenta fue desactivada o cambió de rol, la sesión se invalida de inmediato.
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

        String uri = request.getRequestURI();
        // No interceptar peticiones a la API REST (se manejan vía JWT y exceptionHandling JSON)
        if (uri.startsWith("/api/") || uri.contains("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Solo verificar si hay usuario autenticado en sesión web
        String authHeader = request.getHeader("Authorization");
        boolean esPeticionJwt = authHeader != null && authHeader.startsWith("Bearer ");

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()) && !esPeticionJwt) {
            String username = auth.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

            if (usuarioOpt.isEmpty() || (usuarioOpt.get().getActivo() != null && !usuarioOpt.get().getActivo())) {
                // Cuenta eliminada o desactivada -> Cerrar sesión inmediatamente
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect("/login?error=inactivo");
                return;
            }

            // Comprobar si su rol cambió en la BD respecto al rol de su sesión
            Usuario usuario = usuarioOpt.get();
            String rolEsperado = "ROLE_" + usuario.getRol().name();
            boolean tieneRolValido = auth.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals(rolEsperado));

            if (!tieneRolValido) {
                // El rol del usuario fue modificado por el Gestor Técnico -> invalidar sesión actual
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect("/login?error=rol_actualizado");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
