package ar.edu.uade.api.deliverygo.usuarios.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Credencial propia de cada módulo consumidor (RN-15): una clave por módulo, configurada por variable de
// entorno (app.modules.api-keys.<modulo>). Hay dos formas de usarla: presentarla una sola vez en
// POST /api/v1/auth/module-token ("un rol y una key", clase 10) y recibir un JWT con rol MODULO para usar como
// Bearer (la convención del TPO), o mandarla en X-Api-Key en cada llamada (compatibilidad con los módulos que
// ya la usan).
// Una clave vacía o de menos de 16 caracteres deja al módulo sin acceso: nunca hay una credencial por defecto.
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
                log.warn("La clave del módulo '{}' tiene menos de {} caracteres y se ignora", modulo, LARGO_MINIMO);
            }
        });
    }

    // Devuelve el módulo dueño de la clave presentada, o vacío si ninguna coincide. Es la forma alternativa
    // de identificarse: la clave en X-Api-Key en cada llamada (ver ApiKeyAuthenticationFilter).
    public Optional<String> identificar(String presentada) {
        if (presentada == null || presentada.isBlank()) {
            return Optional.empty();
        }
        byte[] buscada = presentada.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entrada : apiKeys.entrySet()) {
            String clave = entrada.getValue();
            if (clave != null && clave.length() >= LARGO_MINIMO
                    && MessageDigest.isEqual(clave.getBytes(StandardCharsets.UTF_8), buscada)) {
                return Optional.of(entrada.getKey());
            }
        }
        return Optional.empty();
    }

    // Verdadero si el módulo existe, tiene una clave válida configurada y coincide con la presentada.
    public boolean verificar(String modulo, String presentada) {
        if (modulo == null || presentada == null) {
            return false;
        }
        String clave = apiKeys.get(modulo.trim().toLowerCase());
        if (clave == null || clave.length() < LARGO_MINIMO) {
            return false;
        }
        return MessageDigest.isEqual(clave.getBytes(StandardCharsets.UTF_8), presentada.getBytes(StandardCharsets.UTF_8));
    }
}
