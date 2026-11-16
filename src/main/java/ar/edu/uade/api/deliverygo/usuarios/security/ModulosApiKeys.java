package ar.edu.uade.api.deliverygo.usuarios.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    // Devuelve el módulo dueño de la clave presentada, o vacío si ninguna coincide.
    public Optional<String> identificar(String presentada) {
        if (presentada == null || presentada.isBlank()) {
            return Optional.empty();
        }
        for (Map.Entry<String, String> entrada : apiKeys.entrySet()) {
            String clave = entrada.getValue();
            if (clave != null && clave.length() >= LARGO_MINIMO && clave.equals(presentada)) {
                return Optional.of(entrada.getKey());
            }
        }
        return Optional.empty();
    }
}
