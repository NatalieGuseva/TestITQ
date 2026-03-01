package org.example.service.exception;


public class DocumentNotFoundException extends RuntimeException {

    private final Long documentId;

    public DocumentNotFoundException(Long documentId) {
        super("Документ с id=" + documentId + " не найден");
        this.documentId = documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }
}