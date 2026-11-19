package pe.edu.utp.Grupo04.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtService {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException("JWT Secret no está configurado o no cumple con la longitud mínima de seguridad (al menos 256 bits). Configure app.jwt.secret / JWT_SECRET.");
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(String username, String rol) {
        return generarToken(username, rol, 1L);
    }

    public String generarToken(String username, String rol, long tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        claims.put("tokenVersion", tokenVersion);
        return generarTokenConClaims(claims, username);
    }

    public String generarTokenConClaims(Map<String, Object> extraClaims, String username) {
        Date ahora = new Date();
        Date fechaExpiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(getSigningKey())
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public String extraerRol(String token) {
        Claims claims = extraerTodosLosClaims(token);
        Object rol = claims.get("rol");
        return rol != null ? rol.toString() : null;
    }

    public Long extraerTokenVersion(String token) {
        Claims claims = extraerTodosLosClaims(token);
        Object ver = claims.get("tokenVersion");
        if (ver instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosLosClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esTokenValido(String token, String usernameEsperado) {
        try {
            final String username = extraerUsername(token);
            return (username.equalsIgnoreCase(usernameEsperado) && !estaTokenExpirado(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean estaTokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }
}
