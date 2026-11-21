package pe.edu.utp.Grupo04.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.edu.utp.Grupo04.security.SessionValidityFilter;
import pe.edu.utp.Grupo04.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SessionValidityFilter sessionValidityFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SessionValidityFilter sessionValidityFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.sessionValidityFilter = sessionValidityFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isTecnico = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_GESTOR_TECNICO"));
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMINISTRACION"));
            boolean isRecepcion = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_RECEPCION"));
            boolean isCliente = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_CLIENTE"));

            if (isTecnico) {
                response.sendRedirect("/tecnico/cuentas");
            } else if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else if (isRecepcion) {
                response.sendRedirect("/admin/socios");
            } else if (isCliente) {
                response.sendRedirect("/cliente/mi-membresia");
            } else {
                response.sendRedirect("/inicio");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // Feedback #5 / M10: Las operaciones con cookies mantienen CSRF obligatorio.
                // Excluir únicamente los endpoints de la API REST que usan autenticación JWT (/api/auth/** y endpoints stateless)
                .ignoringRequestMatchers("/api/auth/**", "/api/jwt/**")
            )
            .authorizeHttpRequests(auth -> auth
                // 1. Recursos estáticos públicos y ruta de error
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()

                // 2. Vistas públicas y catálogo público
                .requestMatchers("/", "/inicio", "/planes", "/promociones", "/productos", "/afluencia", "/detalle-producto").permitAll()

                // 3. Autenticación pública y login JWT
                .requestMatchers("/login", "/registrarse", "/registro", "/register", "/api/auth/login").permitAll()

                // 4. API delimitada JWT demostrable (S15 Sesión 30) - Requiere token JWT
                .requestMatchers("/api/auth/me", "/api/jwt/**").authenticated()

                // 5. Módulo exclusivo del GESTOR TÉCNICO (Cuentas y roles - RBAC)
                // Criterio de aislamiento estricto: El Gestor Técnico NO accede a compras ni datos de negocio
                .requestMatchers("/tecnico/**").hasRole("GESTOR_TECNICO")

                // Feedback #3: Bloquear al Gestor Técnico de realizar compras ficticias y módulos operativos
                .requestMatchers(HttpMethod.POST, "/api/compras/**", "/cliente/api/compras/**").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/compras/**", "/cliente/api/compras/**").hasAnyRole("CLIENTE", "RECEPCION", "ADMINISTRACION")
                .requestMatchers(HttpMethod.GET, "/api/boleta-venta/descargar", "/boleta-venta/**",
                        "/cliente/boleta-venta/**", "/cliente/boleta/descargar")
                        .hasAnyRole("CLIENTE", "RECEPCION", "ADMINISTRACION")

                // 6. Módulo de CLIENTE (Socios)
                .requestMatchers("/cliente/**").hasRole("CLIENTE")

                // 7. Módulo de GESTIÓN OPERATIVA Y ADMINISTRATIVA (Matriz de permisos de 4 roles)

                // 7.1 AFLUENCIA: Recepción registra/actualiza; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/afluencia/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/afluencia/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.2 SOCIOS: Recepción registra, consulta y actualiza datos; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/socios/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/socios/*/editar").hasRole("RECEPCION")
                .requestMatchers("/api/admin/socios/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/socios/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.3 MEMBRESÍAS: Recepción asigna/gestiona; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/membresias/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/membresias/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.4 PAGOS: Recepción registra pagos presenciales y entregas; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/pagos/**", "/admin/api/compras/*/entregar").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/pagos/**", "/admin/api/compras/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.5 RESERVAS: Recepción atiende reservas y gestiona estado; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/api/reservas").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.POST, "/admin/reservas/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/reservas/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.6 PRODUCTOS / INVENTARIO: Administración gestiona catálogo, precios y stock; Recepción solo consulta disponibilidad
                .requestMatchers(HttpMethod.POST, "/admin/productos/**").hasRole("ADMINISTRACION")
                .requestMatchers(HttpMethod.GET, "/admin/productos/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.7 PLANES: Administración gestiona; Recepción solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/planes/**").hasRole("ADMINISTRACION")
                .requestMatchers(HttpMethod.GET, "/admin/planes/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.8 PROMOCIONES: Administración gestiona; Recepción solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/promociones/**").hasRole("ADMINISTRACION")
                .requestMatchers(HttpMethod.GET, "/admin/promociones/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.9 DASHBOARD E INDICADORES DE NEGOCIO: Exclusivo de Administración
                .requestMatchers("/admin", "/admin/dashboard").hasRole("ADMINISTRACION")

                // 7.10 EQUIPOS Y MANTENIMIENTOS: Exclusivo de Administración
                .requestMatchers("/admin/equipos/**", "/admin/mantenimientos/**", "/admin/reporte-mantenimiento/**").hasRole("ADMINISTRACION")

                // Cualquier otra subruta dentro de /admin requiere ser ADMINISTRACION o RECEPCION
                .requestMatchers("/admin/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 8. Cualquier otra solicitud requiere autenticación
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"No autorizado\",\"mensaje\":\"" + authException.getMessage() + "\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/") || uri.contains("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Acceso denegado\",\"mensaje\":\"" + accessDeniedException.getMessage() + "\"}");
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
                    }
                })
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(customAuthenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        // Filtro de validación activa de sesión (verifica revocación inmediata por cambio de rol o desactivación)
        http.addFilterAfter(sessionValidityFilter, UsernamePasswordAuthenticationFilter.class);

        // Filtro JWT para peticiones con token Bearer
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
