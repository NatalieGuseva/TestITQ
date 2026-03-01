package org.example.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.service.dto.response.ConcurrencyTestResponse;
import org.example.service.service.ConcurrencyTestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
@Tag(name = "Concurrency Test", description = "Проверка конкурентного утверждения документа")
public class ConcurrencyTestController {

    private final ConcurrencyTestService concurrencyTestService;

    /**
     * 6) Проверка конкурентного утверждения
     * Запускает threads потоков, каждый делает attempts попыток утвердить один документ.
     * Ожидаемое поведение: ровно одна попытка переводит в APPROVED, остальные — конфликт.
     */
    @PostMapping("/{id}/concurrency-test")
    @Operation(
            summary = "Тест конкурентного утверждения",
            description = "Запускает несколько параллельных попыток утвердить один документ. " +
                    "Ожидаемое поведение: ровно одна попытка завершается успехом (APPROVED + запись в реестре), " +
                    "остальные завершаются конфликтом без изменений. " +
                    "Возвращает сводку: успешных / конфликтов / ошибок, финальный статус документа."
    )
    public ConcurrencyTestResponse testConcurrentApproval(
            @PathVariable Long id,

            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Минимальное количество потоков: 1")
            @Max(value = 20, message = "Максимальное количество потоков: 20")
            int threads,

            @RequestParam(defaultValue = "3")
            @Min(value = 1, message = "Минимальное количество попыток: 1")
            @Max(value = 10, message = "Максимальное количество попыток: 10")
            int attempts,

            @RequestParam(defaultValue = "concurrency-tester")
            String initiator
    ) {
        return concurrencyTestService.testConcurrentApproval(id, threads, attempts, initiator);
    }
}
