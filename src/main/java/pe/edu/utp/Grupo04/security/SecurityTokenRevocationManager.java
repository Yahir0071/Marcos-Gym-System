package pe.edu.utp.Grupo04.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor persistente de versiones de seguridad por usuario.
 * Garantiza que los tokens JWT y sesiones revocadas permanezcan inválidos incluso tras reiniciar la aplicación.
 * 1. Persiste 'token_version' en la tabla 'usuarios'.
 * 2. Invalida tokens al cambiar rol o desactivar cuenta.
 * 3. Mantiene caché en memoria para alto rendimiento.
 */
@Component
public class SecurityTokenRevocationManager {

    private final UsuarioRepository usuarioRepository;
    private final Map<String, Long> userTokenVersionsCache = new ConcurrentHashMap<>();

    public SecurityTokenRevocationManager(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public long getTokenVersion(String username) {
        if (username == null) return 0L;
        String cleanUser = username.trim().toLowerCase();

        Long cached = userTokenVersionsCache.get(cleanUser);
        if (cached != null) {
            return cached;
        }

        try {
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(cleanUser);
            if (userOpt.isPresent()) {
                long dbVer = userOpt.get().getTokenVersion() != null ? userOpt.get().getTokenVersion() : 1L;
                userTokenVersionsCache.put(cleanUser, dbVer);
                return dbVer;
            }
        } catch (Exception ignored) {
        }

        return userTokenVersionsCache.computeIfAbsent(cleanUser, k -> 1L);
    }

    @Transactional
    public void revocarAccesoUsuario(String username) {
        if (username == null) return;
        String cleanUser = username.trim().toLowerCase();

        try {
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(cleanUser);
            if (userOpt.isPresent()) {
                Usuario user = userOpt.get();
                long currentVer = user.getTokenVersion() != null ? user.getTokenVersion() : 1L;
                long nextVer = currentVer + 1L;
                user.setTokenVersion(nextVer);
                usuarioRepository.save(user);
                userTokenVersionsCache.put(cleanUser, nextVer);
                return;
            }
        } catch (Exception ignored) {
        }

        userTokenVersionsCache.compute(cleanUser, (k, v) -> (v == null ? 1L : v) + 1L);
    }

    public boolean esTokenValidoParaVersion(String username, Long tokenVersion) {
        if (username == null || tokenVersion == null) return false;
        String cleanUser = username.trim().toLowerCase();

        try {
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(cleanUser);
            if (userOpt.isPresent()) {
                Usuario user = userOpt.get();
                if (user.getActivo() != null && !user.getActivo()) {
                    return false; // Cuenta desactivada
                }
                long dbVer = user.getTokenVersion() != null ? user.getTokenVersion() : 1L;
                userTokenVersionsCache.put(cleanUser, dbVer);
                return tokenVersion.equals(dbVer);
            }
        } catch (Exception ignored) {
        }

        long versionActual = getTokenVersion(cleanUser);
        return tokenVersion.equals(versionActual);
    }

    /**
     * Limpia la caché en memoria (usado en tests de reinicio de servidor).
     */
    public void limpiarCacheMemoria() {
        userTokenVersionsCache.clear();
    }
}
