package org.example.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOperationResult {

    private Long id;
    private OperationStatus status;
    private String message;

    public enum OperationStatus {
        SUCCESS,        // операция выполнена успешно
        CONFLICT,       // недопустимый переход статуса
        NOT_FOUND,      // документ не найден
        REGISTRY_ERROR  // ошибка записи в реестр утверждений (только для approve)
    }

    // Фабричные методы для удобства

    public static DocumentOperationResult success(Long id) {
        return DocumentOperationResult.builder()
                .id(id)
                .status(OperationStatus.SUCCESS)
                .message("Операция выполнена успешно")
                .build();
    }

    public static DocumentOperationResult conflict(Long id, String message) {
        return DocumentOperationResult.builder()
                .id(id)
                .status(OperationStatus.CONFLICT)
                .message(message)
                .build();
    }

    public static DocumentOperationResult notFound(Long id) {
        return DocumentOperationResult.builder()
                .id(id)
                .status(OperationStatus.NOT_FOUND)
                .message("Документ с id=" + id + " не найден")
                .build();
    }

    public static DocumentOperationResult registryError(Long id, String message) {
        return DocumentOperationResult.builder()
                .id(id)
                .status(OperationStatus.REGISTRY_ERROR)
                .message(message)
                .build();
    }
}
