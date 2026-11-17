package ar.edu.uade.api.deliverygo.usuarios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

// INT-01 e INT-06 en segundo plano: la respuesta al cliente no espera a Notificaciones. Así el tiempo de
// POST /auth/password/forgot no delata si el email existe (RN-13) y el registro no depende de otro módulo.
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String EJECUTOR_NOTIFICACIONES = "notificacionesExecutor";

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = EJECUTOR_NOTIFICACIONES)
    public ThreadPoolTaskExecutor notificacionesExecutor() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("notificaciones-");
        ejecutor.setTaskDecorator(copiarMdc());
        return ejecutor;
    }

    // El hilo del envío no hereda el MDC: se copia el de la solicitud para conservar el X-Correlation-Id.
    static TaskDecorator copiarMdc() {
        return tarea -> {
            Map<String, String> contexto = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previo = MDC.getCopyOfContextMap();
                if (contexto == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(contexto);
                }
                try {
                    tarea.run();
                } finally {
                    if (previo == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previo);
                    }
                }
            };
        };
    }

    // Una falla en segundo plano ya no llega a quien llamó: se registra acá, sin cambiar ninguna respuesta.
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (error, metodo, parametros) ->
                log.warn("Falló en segundo plano {}: {}", metodo.getName(), error.getMessage(), error);
    }
}
