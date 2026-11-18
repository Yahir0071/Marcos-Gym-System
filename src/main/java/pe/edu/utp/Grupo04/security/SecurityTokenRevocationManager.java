package pe.edu.utp.Grupo04.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor en memoria de versiones de seguridad por usuario.
 * Permite invalidar inmediatamente tokens JWT y sesiones previas cuando:
 * 1. Se desactiva la cuenta del usuario.
 * 2. Se cambia el rol del usuario.
 */
@Component
public class SecurityTokenRevocationManager {

    // Guarda el tokenVersion actual por cada username
    private final Map<String, Long> userTokenVersions = new ConcurrentHashMap<>();

    public long getTokenVersion(String username) {
        if (username == null) return 0L;
        return userTokenVersions.computeIfAbsent(username.toLowerCase(), k -> 1L);
    }

    public void revocarAccesoUsuario(String username) {
        if (username != null) {
            userTokenVersions.compute(username.toLowerCase(), (k, v) -> (v == null ? 1L : v) + 1L);
        }
    }

    public boolean esTokenValidoParaVersion(String username, Long tokenVersion) {
        if (username == null || tokenVersion == null) return false;
        long versionActual = getTokenVersion(username);
        return tokenVersion.equals(versionActual);
    }
}
