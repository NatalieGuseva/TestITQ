package org.example.document.generator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.document.generator.client.DocumentApiClient;
import org.example.document.generator.config.GeneratorConfig;
import org.example.document.generator.dto.CreateDocumentRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGeneratorService {

    private final DocumentApiClient apiClient;
    private final GeneratorConfig config;

    public void generate(int count, String author) {
        int total = count > 0 ? count : config.getCount();
        int batchSize = config.getBatchSize();

        log.info("=== Запуск генерации: всего документов к созданию = {} ===", total);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        Instant start = Instant.now();

        Flux.range(1, total)
                .flatMap(i -> {
                    CreateDocumentRequest request = buildRequest(i, author);
                    return apiClient.createDocument(request)
                            .doOnSuccess(doc -> {
                                if (doc != null) {
                                    int done = success.incrementAndGet();
                                    if (done % batchSize == 0 || done == total) {
                                        log.info("Прогресс: создано {}/{} документов", done, total);
                                    }
                                } else {
                                    failed.incrementAndGet();
                                }
                            });
                }, 10)
                .blockLast();

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("=== Генерация завершена: успешно={}, ошибок={}, время={} мс ===",
                success.get(), failed.get(), elapsed.toMillis());
    }

    private CreateDocumentRequest buildRequest(int index, String author) {
        return CreateDocumentRequest.builder()
                .title("Document-" + index + "-" + UUID.randomUUID().toString().substring(0, 8))
                .author(author)
                .initiator("generator")
                .build();
    }
}