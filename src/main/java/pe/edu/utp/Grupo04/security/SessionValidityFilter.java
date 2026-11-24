package pe.edu.utp.Grupo04.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.utp.Grupo04.config.SecurityConfig;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;
import pe.edu.utp.Grupo04.security.jwt.JwtAuthenticationFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class SessionValidityFilter extends OncePerRequestFilter {
    private final AntPathMatcher rutas = new AntPathMatcher();
    private final UsuarioRepository usuarioRepository;

    public SessionValidityFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return Arrays.stream(SecurityConfig.RECURSOS_PUBLICOS)
                .anyMatch(recurso -> rutas.match(recurso, ApiSecurityError.ruta(request)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean jwtVerificado = Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.JWT_VERIFICADO));
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(auth.getName()) || jwtVerificado) {
            filterChain.doFilter(request, response);
            return;
        }

        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        boolean inactivo = usuario == null || !Boolean.TRUE.equals(usuario.getActivo());
        boolean rolVigente = !inactivo && auth.getAuthorities().stream()
                .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_" + usuario.getRol().name()));
        if (rolVigente) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        boolean paginaPublica = "GET".equals(request.getMethod())
                && Arrays.asList(SecurityConfig.PAGINAS_PUBLICAS).contains(ApiSecurityError.ruta(request));
        if (paginaPublica) {
            filterChain.doFilter(request, response);
        } else if (ApiSecurityError.esApi(request)) {
            ApiSecurityError.UNAUTHORIZED.responder(response, HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            String causa = inactivo ? "inactivo" : "rol_actualizado";
            response.sendRedirect(request.getContextPath() + "/login?error=" + causa);
        }
    }
}
