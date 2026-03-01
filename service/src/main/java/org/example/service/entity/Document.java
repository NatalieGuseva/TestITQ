package org.example.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(name = "idx_documents_status", columnList = "status"),
                @Index(name = "idx_documents_author", columnList = "author"),
                @Index(name = "idx_documents_created_at", columnList = "created_at"),
                @Index(name = "idx_documents_number", columnList = "number", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documents_seq")
    @SequenceGenerator(name = "documents_seq", sequenceName = "documents_seq", allocationSize = 1)
    private Long id;

    /**
     * Уникальный номер документа — генерируется при создании.
     * Формат: DOC-{год}-{порядковый номер}, например DOC-2025-00001
     */
    @Column(name = "number", nullable = false, unique = true, length = 50)
    private String number;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "author", nullable = false, length = 255)
    private String author;

    /**
     * Статус документа. Используем optimistic locking через @Version
     * чтобы предотвратить конкурентное изменение статуса.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.DRAFT;

    /**
     * Версия для optimistic locking — гарантирует что при конкурентном
     * утверждении только одна транзакция победит, остальные получат конфликт.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("performedAt ASC")
    private List<DocumentHistory> history = new ArrayList<>();
}
