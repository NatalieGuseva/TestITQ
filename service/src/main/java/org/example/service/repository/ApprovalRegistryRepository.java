package org.example.service.repository;


import org.example.service.entity.ApprovalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {

    /**
     * Проверить существование записи в реестре по документу.
     * Используется для защиты от двойного утверждения.
     */
    boolean existsByDocumentId(Long documentId);

    /**
     * Найти запись реестра по документу.
     */
    Optional<ApprovalRegistry> findByDocumentId(Long documentId);
}