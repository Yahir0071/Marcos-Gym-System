package pe.edu.utp.Grupo04.config;

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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.edu.utp.Grupo04.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                // M10: Proteger con CSRF todas las operaciones con sesión web.
                // Excluir únicamente los endpoints de la API REST protegidos por JWT (/api/auth/** y /api/** stateless)
                .ignoringRequestMatchers("/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                // 1. Recursos estáticos públicos y ruta de error
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()

                // 2. Vistas públicas y catálogo público
                .requestMatchers("/", "/inicio", "/planes", "/promociones", "/productos", "/afluencia", "/detalle-producto").permitAll()

                // 3. Autenticación pública y API de autenticación JWT
                .requestMatchers("/login", "/registrarse", "/registro", "/register", "/api/auth/**").permitAll()

                // 4. API delimitada JWT demostrable (S15 Sesión 30)
                .requestMatchers("/api/auth/me").authenticated()

                // 5. Módulo exclusivo del GESTOR TÉCNICO (Cuentas y roles - RBAC)
                // Criterio de aislamiento: No accede a datos de negocio
                .requestMatchers("/tecnico/**").hasRole("GESTOR_TECNICO")

                // 6. Módulo de CLIENTE (Socios)
                .requestMatchers("/cliente/**").hasRole("CLIENTE")

                // 7. Módulo de GESTIÓN OPERATIVA Y ADMINISTRATIVA (Matriz de permisos de 4 roles)

                // 7.1 AFLUENCIA: Recepción registra/actualiza; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/afluencia/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/afluencia/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.2 SOCIOS: Recepción registra, consulta y actualiza datos; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/socios/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/socios/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.3 MEMBRESÍAS: Recepción asigna/gestiona; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/membresias/**").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/membresias/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.4 PAGOS: Recepción registra pagos presenciales y entregas; Administración solo consulta
                .requestMatchers(HttpMethod.POST, "/admin/pagos/**", "/admin/api/compras/*/entregar").hasRole("RECEPCION")
                .requestMatchers(HttpMethod.GET, "/admin/pagos/**", "/admin/api/compras/**").hasAnyRole("ADMINISTRACION", "RECEPCION")

                // 7.5 RESERVAS: Recepción atiende reservas y gestiona estado; Administración solo consulta
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

        // Añadir filtro JWT antes del filtro de usuario y contraseña para procesar tokens Bearer en peticiones a la API
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
