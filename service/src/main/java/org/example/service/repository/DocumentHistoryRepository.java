package org.example.service.repository;

import org.example.service.entity.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

    /**
     * Получить полную историю документа, отсортированную по времени.
     */
    List<DocumentHistory> findByDocumentIdOrderByPerformedAtAsc(Long documentId);

    /**
     * Получить историю для нескольких документов сразу (для пакетных операций).
     * Загружаем сразу чтобы избежать N+1 проблемы.
     */
    @Query("SELECT h FROM DocumentHistory h WHERE h.document.id IN :documentIds ORDER BY h.performedAt ASC")
    List<DocumentHistory> findByDocumentIdInOrderByPerformedAtAsc(@Param("documentIds") List<Long> documentIds);
}
