package org.example.service.exception;


import org.example.service.entity.DocumentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(Long documentId, DocumentStatus current, DocumentStatus expected) {
        super(String.format(
                "Недопустимый переход статуса для документа id=%d: текущий=%s, ожидался=%s",
                documentId, current, expected
        ));
    }
}
