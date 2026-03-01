package org.example.service.dto.request;

import lombok.Data;
import org.example.service.entity.DocumentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DocumentSearchRequest {

    // Фильтр по статусу (опционально)
    private DocumentStatus status;

    // Фильтр по автору (опционально)
    private String author;

    // Период дат создания документа (опционально)
    // Трактуется по дате создания (createdAt)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
}
