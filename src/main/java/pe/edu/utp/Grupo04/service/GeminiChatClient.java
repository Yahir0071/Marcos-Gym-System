package pe.edu.utp.Grupo04.service;

import java.util.Optional;

public interface GeminiChatClient {

    boolean estaConfigurado();

    Optional<String> generarRespuesta(String systemPrompt, String contextoGym, String mensajeUsuario);
}
