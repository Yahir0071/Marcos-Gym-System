package pe.edu.utp.Grupo04.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import pe.edu.utp.Grupo04.security.SessionValidityFilter;
import pe.edu.utp.Grupo04.security.ApiSecurityError;
import pe.edu.utp.Grupo04.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    public static final String[] RECURSOS_PUBLICOS = {"/css/**", "/js/**", "/img/**", "/favicon.ico", "/error"};
    public static final String[] PAGINAS_PUBLICAS = {"/", "/inicio", "/planes", "/promociones", "/productos",
            "/afluencia", "/detalle-producto", "/public/detalle-producto", "/login", "/registrarse", "/registro", "/register"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SessionValidityFilter sessionValidityFilter;
    private final HttpSessionRequestCache destinosLogin = new HttpSessionRequestCache();

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SessionValidityFilter sessionValidityFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.sessionValidityFilter = sessionValidityFilter;
        destinosLogin.setRequestMatcher(request -> HttpMethod.GET.matches(request.getMethod())
                && "/cliente/detalle-producto".equals(ApiSecurityError.ruta(request))
                && idProducto(request.getParameter("id")) != null);
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
    public FilterRegistrationBean<JwtAuthenticationFilter> registrarJwtSoloEnSpringSecurity(JwtAuthenticationFilter filtro) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<SessionValidityFilter> registrarSesionSoloEnSpringSecurity(SessionValidityFilter filtro) {
        FilterRegistrationBean<SessionValidityFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var destino = destinosLogin.getRequest(request, response);
            String[] valores = destino == null ? null : destino.getParameterValues("id");
            Long producto = valores != null && valores.length == 1 ? idProducto(valores[0]) : null;
            destinosLogin.removeRequest(request, response);
            String baseUrl = request.getContextPath();
            boolean isTecnico = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_GESTOR_TECNICO"));
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMINISTRACION"));
            boolean isRecepcion = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_RECEPCION"));
            boolean isCliente = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_CLIENTE"));

            if (isTecnico) {
                response.sendRedirect(baseUrl + "/tecnico/cuentas");
            } else if (isAdmin) {
                response.sendRedirect(baseUrl + "/admin/dashboard");
            } else if (isRecepcion) {
                response.sendRedirect(baseUrl + "/admin/socios");
            } else if (isCliente) {
                response.sendRedirect(baseUrl + (producto == null
                        ? "/cliente/mi-membresia" : "/cliente/detalle-producto?id=" + producto));
            } else {
                response.sendRedirect(baseUrl + "/inicio");
            }
        };
    }

    private static Long idProducto(String parametro) {
        if (parametro == null || !parametro.matches("[1-9][0-9]{0,18}")) {
            return null;
        }
        try {
            return Long.valueOf(parametro);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var login = new LoginUrlAuthenticationEntryPoint("/login");
        http
            .requestCache(cache -> cache.requestCache(destinosLogin))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")))
            .csrf(csrf -> csrf
                // Feedback #5 / M10: Las operaciones con cookies mantienen CSRF obligatorio.
                // Excluir únicamente los endpoints de la API REST que usan autenticación JWT (/api/auth/** y endpoints stateless)
                .ignoringRequestMatchers("/api/auth/**", "/api/jwt/**")
            )
            .authorizeHttpRequests(auth -> auth
                // 1. Recursos estáticos públicos y ruta de error
                .requestMatchers(RECURSOS_PUBLICOS).permitAll()

                // 2. Vistas públicas y catálogo público
                .requestMatchers(HttpMethod.GET, PAGINAS_PUBLICAS).permitAll()

                // 3. Autenticación pública, login JWT y Chatbot (con CSRF obligatorio en POST)
                .requestMatchers("/login", "/registrarse", "/registro", "/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/chatbot/**").permitAll()

                // 4. API delimitada JWT demostrable (S15 Sesión 30) - Requiere token JWT
                .requestMatchers("/api/auth/me", "/api/jwt/**").authenticated()

                // 5. Módulo exclusivo del GESTOR TÉCNICO (Cuentas y roles - RBAC)
                // Criterio de aislamiento estricto: El Gestor Técnico NO accede a compras ni datos de negocio
                .requestMatchers("/tecnico/**").hasRole("GESTOR_TECNICO")

                // Feedback #3: Bloquear al Gestor Técnico de realizar compras ficticias y módulos operativos
                .requestMatchers(HttpMethod.POST, "/api/compras/**", "/cliente/api/compras/**").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/compras/**", "/cliente/api/compras/**").hasAnyRole("CLIENTE", "RECEPCION", "ADMINISTRACION")
                .requestMatchers(HttpMethod.GET, "/api/boleta-venta/descargar",
                        "/cliente/boleta-venta/**", "/cliente/boleta-venta", "/cliente/boleta/descargar").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/admin/boleta-venta/descargar")
                        .hasAnyRole("RECEPCION", "ADMINISTRACION")

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
                    if (ApiSecurityError.esApi(request)) {
                        ApiSecurityError.UNAUTHORIZED.responder(response, HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        login.commence(request, response, authException);
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (ApiSecurityError.esApi(request)) {
                        ApiSecurityError.FORBIDDEN.responder(response, HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
