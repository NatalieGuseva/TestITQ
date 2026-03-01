package org.example.service.dto.response;

import lombok.Data;
import org.example.service.entity.DocumentStatus;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {

    private Long id;
    private String number;
    private String title;
    private String author;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}