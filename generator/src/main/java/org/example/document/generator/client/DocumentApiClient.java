package org.example.document.generator.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.document.generator.config.GeneratorConfig;
import org.example.document.generator.dto.CreateDocumentRequest;
import org.example.document.generator.dto.DocumentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentApiClient {

    private final WebClient webClient;
    private final GeneratorConfig config;

    /**
     * Создаёт документ через API сервиса.
     * Возвращает пустой Mono при ошибке (не бросает исключение, чтобы не останавливать генерацию).
     */
    public Mono<DocumentResponse> createDocument(CreateDocumentRequest request) {
        return webClient.post()
                .uri("/api/v1/documents")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DocumentResponse.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .doOnSuccess(doc -> log.debug("Документ создан: id={}, number={}", doc.getId(), doc.getNumber()))
                .onErrorResume(ex -> {
                    log.error("Ошибка при создании документа [title={}]: {}", request.getTitle(), ex.getMessage());
                    return Mono.empty();
                });
    }
}
