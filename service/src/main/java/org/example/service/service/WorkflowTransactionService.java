package org.example.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.dto.response.DocumentOperationResult;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentHistory;
import org.example.service.entity.DocumentStatus;
import org.example.service.entity.HistoryAction;
import org.example.service.repository.DocumentHistoryRepository;
import org.example.service.repository.DocumentRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTransactionService {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository documentHistoryRepository;
    private final ApprovalRegistryService approvalRegistryService;

    @Transactional
    public DocumentOperationResult submitOne(Long id, String initiator, String comment) {
        try {
            Document document = documentRepository.findByIdWithLock(id).orElse(null);

            if (document == null) {
                return DocumentOperationResult.notFound(id);
            }

            if (document.getStatus() != DocumentStatus.DRAFT) {
                log.warn("Submit конфликт: документ id={} имеет статус {}, ожидался DRAFT",
                        id, document.getStatus());
                return DocumentOperationResult.conflict(id,
                        "Документ находится в статусе " + document.getStatus() + ", ожидался DRAFT");
            }

            document.setStatus(DocumentStatus.SUBMITTED);
            documentRepository.save(document);

            DocumentHistory history = DocumentHistory.of(document, HistoryAction.SUBMIT, initiator, comment);
            documentHistoryRepository.save(history);

            log.info("Документ id={} переведён в SUBMITTED инициатором={}", id, initiator);
            return DocumentOperationResult.success(id);

        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Оптимистичная блокировка при submit документа id={}", id);
            return DocumentOperationResult.conflict(id, "Конкурентное изменение документа, попробуйте ещё раз");
        } catch (Exception ex) {
            log.error("Ошибка при submit документа id={}: {}", id, ex.getMessage());
            return DocumentOperationResult.conflict(id, "Внутренняя ошибка: " + ex.getMessage());
        }
    }

    @Transactional
    public DocumentOperationResult approveOne(Long id, String initiator, String comment) {
        try {
            Document document = documentRepository.findByIdWithLock(id).orElse(null);

            if (document == null) {
                return DocumentOperationResult.notFound(id);
            }

            if (document.getStatus() != DocumentStatus.SUBMITTED) {
                log.warn("Approve конфликт: документ id={} имеет статус {}, ожидался SUBMITTED",
                        id, document.getStatus());
                return DocumentOperationResult.conflict(id,
                        "Документ находится в статусе " + document.getStatus() + ", ожидался SUBMITTED");
            }

            document.setStatus(DocumentStatus.APPROVED);
            documentRepository.save(document);

            DocumentHistory history = DocumentHistory.of(document, HistoryAction.APPROVE, initiator, comment);
            documentHistoryRepository.save(history);

            approvalRegistryService.register(document, initiator);

            log.info("Документ id={} утверждён инициатором={}", id, initiator);
            return DocumentOperationResult.success(id);

        } catch (ApprovalRegistryService.RegistryException ex) {
            log.error("Ошибка реестра при approve документа id={}: {}", id, ex.getMessage());
            return DocumentOperationResult.registryError(id, ex.getMessage());
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Оптимистичная блокировка при approve документа id={}", id);
            return DocumentOperationResult.conflict(id, "Конкурентное изменение документа, попробуйте ещё раз");
        } catch (Exception ex) {
            log.error("Ошибка при approve документа id={}: {}", id, ex.getMessage());
            return DocumentOperationResult.conflict(id, "Внутренняя ошибка: " + ex.getMessage());
        }
    }
}