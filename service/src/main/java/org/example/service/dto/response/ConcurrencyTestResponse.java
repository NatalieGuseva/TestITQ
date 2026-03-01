package org.example.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.service.entity.DocumentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrencyTestResponse {

    private Long documentId;
    private int totalAttempts;    // всего попыток (threads * attempts)
    private int succeeded;        // успешных (должно быть ровно 1)
    private int conflicts;        // завершились конфликтом
    private int errors;           // прочие ошибки
    private DocumentStatus finalStatus; // финальный статус документа (должен быть APPROVED)
    private boolean registryCreated;    // создана ли запись в реестре (должна быть ровно одна)
}