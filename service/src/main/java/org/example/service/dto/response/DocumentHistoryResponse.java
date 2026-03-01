package org.example.service.dto.response;

import lombok.Data;
import org.example.service.entity.HistoryAction;

import java.time.LocalDateTime;

@Data
public class DocumentHistoryResponse {

    private Long id;
    private HistoryAction action;      // SUBMIT или APPROVE
    private String performedBy;        // кто выполнил
    private LocalDateTime performedAt; // когда выполнил
    private String comment;            // комментарий (может быть null)
}
