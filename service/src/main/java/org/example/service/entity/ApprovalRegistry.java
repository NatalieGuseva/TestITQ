package org.example.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "approval_registry",
        indexes = {
                @Index(name = "idx_registry_document_id", columnList = "document_id", unique = true),
                @Index(name = "idx_registry_approved_at", columnList = "approved_at"),
                @Index(name = "idx_registry_approved_by", columnList = "approved_by")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "approval_registry_seq")
    @SequenceGenerator(name = "approval_registry_seq", sequenceName = "approval_registry_seq", allocationSize = 1)
    private Long id;

    /**
     * Документ уникален в реестре — нельзя утвердить дважды.
     * unique = true на уровне БД гарантирует это даже при конкурентных запросах.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    /**
     * Номер документа — дублируем для быстрого доступа без JOIN
     */
    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    /**
     * Кто утвердил (инициатор из запроса)
     */
    @Column(name = "approved_by", nullable = false, length = 255)
    private String approvedBy;

    /**
     * Когда утверждено
     */
    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    public static ApprovalRegistry of(Document document, String approvedBy) {
        ApprovalRegistry registry = new ApprovalRegistry();
        registry.setDocument(document);
        registry.setDocumentNumber(document.getNumber());
        registry.setApprovedBy(approvedBy);
        registry.setApprovedAt(LocalDateTime.now());
        return registry;
    }
}