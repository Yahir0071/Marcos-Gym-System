package pe.edu.utp.Grupo04.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.utp.Grupo04.service.GeminiChatClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiHttpChatClient implements GeminiChatClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiHttpChatClient.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final boolean iaHabilitada;

    public GeminiHttpChatClient(
            @Value("${app.chatbot.gemini-api-key:}") String apiKey,
            @Value("${app.chatbot.gemini-model:gemini-2.5-flash}") String model,
            @Value("${app.chatbot.ia-habilitada:true}") boolean iaHabilitada) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = (model == null || model.isBlank()) ? "gemini-2.5-flash" : model.trim();
        this.iaHabilitada = iaHabilitada;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean estaConfigurado() {
        return iaHabilitada && !apiKey.isBlank();
    }

    @Override
    public Optional<String> generarRespuesta(String systemPrompt, String contextoGym, String mensajeUsuario) {
        if (!estaConfigurado()) {
            return Optional.empty();
        }
        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt + "\n\nContexto real de Energy Gym:\n" + contextoGym))
                    ),
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", mensajeUsuario))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 350
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API respondió HTTP {}. Activando fallback local.", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = JSON.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isString() && !textNode.asString().isBlank()) {
                return Optional.of(textNode.asString().trim());
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("No se pudo consultar Gemini API ({}). Activando fallback local.", ex.getMessage());
            return Optional.empty();
        }
    }
}
