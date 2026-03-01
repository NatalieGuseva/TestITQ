package org.example.service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.dto.response.ConcurrencyTestResponse;
import org.example.service.dto.response.DocumentOperationResult;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentStatus;
import org.example.service.exception.DocumentNotFoundException;
import org.example.service.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrencyTestService {

    private final WorkflowTransactionService workflowTransactionService;
    private final DocumentRepository documentRepository;

    /**
     * 6) Запускает threads потоков, каждый делает attempts попыток утвердить документ.
     * Ожидаемое поведение: ровно одна попытка успешна, остальные — конфликт.
     */
    public ConcurrencyTestResponse testConcurrentApproval(Long documentId, int threads,
                                                          int attempts, String initiator) {
        // Проверяем что документ существует
        documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        int totalAttempts = threads * attempts;
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        log.info("Старт теста конкурентного утверждения: documentId={}, threads={}, attempts={}, total={}",
                documentId, threads, attempts, totalAttempts);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1); // все потоки стартуют одновременно
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadNum = t;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // ждём сигнала старта
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (int a = 0; a < attempts; a++) {
                    DocumentOperationResult result = workflowTransactionService.approveOne(
                            documentId,
                            initiator + "-thread-" + threadNum + "-attempt-" + a,
                            "Конкурентный тест"
                    );

                    switch (result.getStatus()) {
                        case SUCCESS -> {
                            succeeded.incrementAndGet();
                            log.info("Поток {} попытка {}: SUCCESS", threadNum, a);
                        }
                        case CONFLICT -> {
                            conflicts.incrementAndGet();
                            log.debug("Поток {} попытка {}: CONFLICT — {}", threadNum, a, result.getMessage());
                        }
                        default -> {
                            errors.incrementAndGet();
                            log.warn("Поток {} попытка {}: {} — {}", threadNum, a,
                                    result.getStatus(), result.getMessage());
                        }
                    }
                }
            }));
        }

        // Запускаем все потоки одновременно
        startLatch.countDown();

        // Ждём завершения всех
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                log.error("Таймаут ожидания потока при тесте конкурентности");
                future.cancel(true);
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Ошибка в потоке конкурентного теста: {}", ex.getMessage());
                errors.incrementAndGet();
            }
        }

        executor.shutdown();

        // Получаем финальный статус документа
        Document finalDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        boolean registryCreated = succeeded.get() == 1
                && finalDocument.getStatus() == DocumentStatus.APPROVED;

        log.info("Тест завершён: documentId={}, успешных={}, конфликтов={}, ошибок={}, финальный статус={}",
                documentId, succeeded.get(), conflicts.get(), errors.get(), finalDocument.getStatus());

        return ConcurrencyTestResponse.builder()
                .documentId(documentId)
                .totalAttempts(totalAttempts)
                .succeeded(succeeded.get())
                .conflicts(conflicts.get())
                .errors(errors.get())
                .finalStatus(finalDocument.getStatus())
                .registryCreated(registryCreated)
                .build();
    }
}
