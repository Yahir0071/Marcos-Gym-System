package pe.edu.utp.Grupo04.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.utp.Grupo04.security.SecurityTokenRevocationManager;
import pe.edu.utp.Grupo04.service.impl.CustomUserDetailsService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityTokenRevocationManager revocationManager;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   SecurityTokenRevocationManager revocationManager) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.revocationManager = revocationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si la petición va dirigida a una API REST protegida por JWT (ej. /api/auth/me o /api/protegida/**)
        // debe aislarse de la sesión web: si no viene con Bearer, no se debe heredar la sesión de cookies.
        String path = request.getRequestURI();
        boolean esEndpointJwtExclusivo = path.startsWith("/api/auth/me") || path.startsWith("/api/jwt/");

        if (esEndpointJwtExclusivo && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            // Forzar contexto limpio sin heredar sesión HTTP previa
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // Si la solicitud normal web no tiene header Authorization Bearer, continuar con la cadena normal (sesión Thymeleaf)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            if (jwtService.validarToken(jwt)) {
                String username = jwtService.extraerUsername(jwt);
                String rol = jwtService.extraerRol(jwt);
                Long tokenVersion = jwtService.extraerTokenVersion(jwt);

                // Feedback #4: Revocar acceso si la versión del token no coincide con el estado actual del usuario
                if (tokenVersion != null && !revocationManager.esTokenValidoParaVersion(username, tokenVersion)) {
                    logger.warn("Token JWT revocado por cambio de rol o desactivación para el usuario: " + username);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Verificar además que el rol en BD coincida con el rol del token (evitar rol desactualizado)
                    String rolActualBd = userDetails.getAuthorities().stream()
                            .findFirst()
                            .map(a -> a.getAuthority().replace("ROLE_", ""))
                            .orElse("");

                    if (rol != null && !rol.equalsIgnoreCase(rolActualBd)) {
                        logger.warn("El rol del token (" + rol + ") no coincide con el rol actual en BD (" + rolActualBd + "). Token rechazado.");
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + rolActualBd)
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Establecer autenticación por JWT para este request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            logger.warn("Fallo de autenticación JWT: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
