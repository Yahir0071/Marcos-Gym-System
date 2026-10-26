package pe.edu.utp.Grupo04.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMINISTRADOR"));
            boolean isCliente = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_CLIENTE"));

            if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
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
                // Eximir /login y endpoints JSON internos del token CSRF para evitar 403 en sesiones demo
                .ignoringRequestMatchers("/login", "/api/**", "/cliente/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos públicos y ruta de error
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                // Vistas públicas
                .requestMatchers("/", "/inicio", "/planes", "/promociones", "/productos", "/afluencia", "/detalle-producto").permitAll()
                // Autenticación pública
                .requestMatchers("/login").permitAll()
                // Módulo de Clientes (Socios) y visualización desde Admin
                .requestMatchers("/cliente/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                // Módulo exclusivo de Administradores
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                // Cualquier otra solicitud requiere autenticación
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

        return http.build();
    }
}
