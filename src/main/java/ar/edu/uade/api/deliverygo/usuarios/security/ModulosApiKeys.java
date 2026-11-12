package ar.edu.uade.api.deliverygo.usuarios.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Credencial propia de cada módulo consumidor (RN-15): una API Key por módulo, configurada por variable de
// entorno (app.modules.api-keys.<modulo>). Una clave vacía o de menos de 16 caracteres deja al módulo
// sin acceso: nunca hay una credencial por defecto.
@Component
@ConfigurationProperties(prefix = "app.modules")
public class ModulosApiKeys {

    static final int LARGO_MINIMO = 16;

    private static final Logger log = LoggerFactory.getLogger(ModulosApiKeys.class);

    private Map<String, String> apiKeys = new LinkedHashMap<>();

    public Map<String, String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Map<String, String> apiKeys) {
        this.apiKeys = apiKeys;
        apiKeys.forEach((modulo, clave) -> {
            if (clave != null && !clave.isBlank() && clave.length() < LARGO_MINIMO) {
                log.warn("La API Key del módulo '{}' tiene menos de {} caracteres y se ignora", modulo, LARGO_MINIMO);
            }
        });
    }

    // Devuelve el módulo dueño de la clave presentada. Compara huellas SHA-256 con tiempo constante y
    // recorre todos los módulos, para que la demora no revele cuál clave se parecía.
    public Optional<String> identificar(String presentada) {
        if (presentada == null || presentada.isBlank()) {
            return Optional.empty();
        }
        byte[] huellaPresentada = sha256(presentada);
        String encontrado = null;
        for (Map.Entry<String, String> entrada : apiKeys.entrySet()) {
            String clave = entrada.getValue();
            if (clave == null || clave.length() < LARGO_MINIMO) {
                continue;
            }
            if (MessageDigest.isEqual(huellaPresentada, sha256(clave))) {
                encontrado = entrada.getKey();
            }
        }
        return Optional.ofNullable(encontrado);
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
