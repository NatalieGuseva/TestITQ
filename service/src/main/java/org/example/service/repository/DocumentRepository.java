package org.example.service.repository;

import jakarta.persistence.LockModeType;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>,
        JpaSpecificationExecutor<Document> {

    /**
     * Получить документ с историей (LEFT JOIN FETCH чтобы избежать N+1).
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.history WHERE d.id = :id")
    Optional<Document> findByIdWithHistory(@Param("id") Long id);

    /**
     * Получить документ с пессимистичной блокировкой для операций изменения статуса.
     * PESSIMISTIC_WRITE гарантирует что только одна транзакция работает с документом.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithLock(@Param("id") Long id);

    /**
     * Пакетное получение документов по списку id с пагинацией.
     */
    Page<Document> findByIdIn(List<Long> ids, Pageable pageable);

    /**
     * Получить страницу документов по статусу (используется в воркерах).
     */
    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.createdAt ASC")
    Page<Document> findPageByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    /**
     * Проверка существования номера документа (для гарантии уникальности при генерации).
     */
    boolean existsByNumber(String number);

    /**
     * Получить следующий порядковый номер для генерации номера документа.
     */
    @Query(value = "SELECT nextval('document_number_seq')", nativeQuery = true)
    long getNextSequence();
}