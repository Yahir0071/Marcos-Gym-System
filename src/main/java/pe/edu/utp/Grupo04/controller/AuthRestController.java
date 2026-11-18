package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.Grupo04.dto.AuthRequestDTO;
import pe.edu.utp.Grupo04.dto.AuthResponseDTO;
import pe.edu.utp.Grupo04.security.SecurityTokenRevocationManager;
import pe.edu.utp.Grupo04.security.jwt.JwtService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityTokenRevocationManager revocationManager;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public AuthRestController(AuthenticationManager authenticationManager,
                              JwtService jwtService,
                              SecurityTokenRevocationManager revocationManager) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.revocationManager = revocationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> autenticarYGenerarToken(@Valid @RequestBody AuthRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_CLIENTE")
                    .replace("ROLE_", "");

            long tokenVersion = revocationManager.getTokenVersion(authentication.getName());
            String token = jwtService.generarToken(authentication.getName(), rol, tokenVersion);

            return ResponseEntity.ok(new AuthResponseDTO(token, authentication.getName(), rol, jwtExpirationMs));
        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Credenciales incorrectas.");
            error.put("mensaje", "El usuario o la contraseña proporcionados no son válidos.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de autenticación.");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("username", authentication.getName());
        respuesta.put("autoridades", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
        return ResponseEntity.ok(respuesta);
    }
}
