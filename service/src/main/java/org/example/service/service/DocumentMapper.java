package org.example.service.service;


import org.example.service.dto.response.DocumentHistoryResponse;
import org.example.service.dto.response.DocumentResponse;
import org.example.service.dto.response.DocumentWithHistoryResponse;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentHistory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setNumber(document.getNumber());
        response.setTitle(document.getTitle());
        response.setAuthor(document.getAuthor());
        response.setStatus(document.getStatus());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    public DocumentWithHistoryResponse toWithHistoryResponse(Document document) {
        DocumentWithHistoryResponse response = new DocumentWithHistoryResponse();
        response.setId(document.getId());
        response.setNumber(document.getNumber());
        response.setTitle(document.getTitle());
        response.setAuthor(document.getAuthor());
        response.setStatus(document.getStatus());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setHistory(toHistoryResponseList(document.getHistory()));
        return response;
    }

    public List<DocumentHistoryResponse> toHistoryResponseList(List<DocumentHistory> history) {
        return history.stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    private DocumentHistoryResponse toHistoryResponse(DocumentHistory history) {
        DocumentHistoryResponse response = new DocumentHistoryResponse();
        response.setId(history.getId());
        response.setAction(history.getAction());
        response.setPerformedBy(history.getPerformedBy());
        response.setPerformedAt(history.getPerformedAt());
        response.setComment(history.getComment());
        return response;
    }
}
