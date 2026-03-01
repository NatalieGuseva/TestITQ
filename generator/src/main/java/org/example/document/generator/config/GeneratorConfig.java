package org.example.document.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "generator")
public class GeneratorConfig {

    /** URL основного сервиса, например http://localhost:8080 */
    private String serviceUrl = "http://localhost:8080";

    /** Количество документов для создания (можно переопределить через CLI) */
    private int count = 10;

    /** Размер пачки при логировании прогресса */
    private int batchSize = 100;

    /** Таймаут HTTP-запроса в секундах */
    private int timeoutSeconds = 30;
}
