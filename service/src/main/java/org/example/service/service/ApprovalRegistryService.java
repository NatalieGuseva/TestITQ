package org.example.service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.entity.ApprovalRegistry;
import org.example.service.entity.Document;
import org.example.service.repository.ApprovalRegistryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRegistryService {

    private final ApprovalRegistryRepository approvalRegistryRepository;

    /**
     * Создать запись в реестре утверждений.
     *
     * Выполняется в REQUIRES_NEW — отдельная транзакция.
     * Если запись уже существует (unique constraint на document_id) —
     * бросает исключение, которое вызывающий код обрабатывает как сигнал к откату утверждения.
     *
     * @throws RegistryException если запись создать не удалось
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApprovalRegistry register(Document document, String approvedBy) {
        try {
            // Проверяем что записи ещё нет (защита от двойного утверждения)
            if (approvalRegistryRepository.existsByDocumentId(document.getId())) {
                throw new RegistryException(
                        "Документ id=" + document.getId() + " уже присутствует в реестре утверждений"
                );
            }

            ApprovalRegistry registry = ApprovalRegistry.of(document, approvedBy);
            ApprovalRegistry saved = approvalRegistryRepository.save(registry);

            log.info("Документ id={} зарегистрирован в реестре утверждений, registry_id={}",
                    document.getId(), saved.getId());
            return saved;

        } catch (DataIntegrityViolationException ex) {
            // Конкурентная вставка — unique constraint сработал на уровне БД
            log.warn("Конкурентная попытка регистрации документа id={} в реестре: {}",
                    document.getId(), ex.getMessage());
            throw new RegistryException(
                    "Не удалось зарегистрировать документ id=" + document.getId() +
                            " в реестре: запись уже существует"
            );
        } catch (Exception ex) {
            log.error("Ошибка при регистрации документа id={} в реестре: {}",
                    document.getId(), ex.getMessage());
            throw new RegistryException(
                    "Ошибка записи в реестр утверждений для документа id=" + document.getId()
            );
        }
    }

    /**
     * Исключение при ошибке записи в реестр.
     * Используется как сигнал к откату утверждения документа.
     */
    public static class RegistryException extends RuntimeException {
        public RegistryException(String message) {
            super(message);
        }
    }
}
