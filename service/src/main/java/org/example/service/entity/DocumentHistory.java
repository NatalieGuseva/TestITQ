package org.example.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_history",
        indexes = {
                @Index(name = "idx_history_document_id", columnList = "document_id"),
                @Index(name = "idx_history_performed_at", columnList = "performed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_history_seq")
    @SequenceGenerator(name = "document_history_seq", sequenceName = "document_history_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * Действие: SUBMIT (DRAFT->SUBMITTED) или APPROVE (SUBMITTED->APPROVED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private HistoryAction action;

    /**
     * Кто выполнил действие (инициатор из запроса)
     */
    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    /**
     * Когда выполнено
     */
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    /**
     * Опциональный комментарий
     */
    @Column(name = "comment", length = 1000)
    private String comment;

    public static DocumentHistory of(Document document, HistoryAction action,
                                     String performedBy, String comment) {
        DocumentHistory history = new DocumentHistory();
        history.setDocument(document);
        history.setAction(action);
        history.setPerformedBy(performedBy);
        history.setPerformedAt(LocalDateTime.now());
        history.setComment(comment);
        return history;
    }
}
