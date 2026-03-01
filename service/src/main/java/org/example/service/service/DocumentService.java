package org.example.service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.dto.request.CreateDocumentRequest;
import org.example.service.dto.request.DocumentSearchRequest;
import org.example.service.dto.response.DocumentResponse;
import org.example.service.dto.response.DocumentWithHistoryResponse;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentStatus;
import org.example.service.exception.DocumentNotFoundException;
import org.example.service.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    /**
     * 1) Создать документ в статусе DRAFT
     */
    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setAuthor(request.getAuthor());
        document.setStatus(DocumentStatus.DRAFT);
        document.setNumber(generateNumber());

        Document saved = documentRepository.save(document);
        log.info("Создан документ: id={}, number={}, author={}", saved.getId(), saved.getNumber(), saved.getAuthor());
        return documentMapper.toResponse(saved);
    }

    /**
     * 2a) Получить документ вместе с историей
     */
    @Transactional(readOnly = true)
    public DocumentWithHistoryResponse getDocumentWithHistory(Long id) {
        Document document = documentRepository.findByIdWithHistory(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return documentMapper.toWithHistoryResponse(document);
    }

    /**
     * 2b) Пакетное получение документов по списку id с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByIds(List<Long> ids, Pageable pageable) {
        return documentRepository.findByIdIn(ids, pageable)
                .map(documentMapper::toResponse);
    }

    /**
     * 5) Поиск документов по фильтрам.
     * Период фильтруется по дате СОЗДАНИЯ (createdAt).
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(DocumentSearchRequest request, Pageable pageable) {
        Specification<Document> spec = buildSearchSpec(request);
        return documentRepository.findAll(spec, pageable)
                .map(documentMapper::toResponse);
    }

    /**
     * Найти документ по id или бросить исключение
     */
    @Transactional(readOnly = true)
    public Document findByIdOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    // --- private ---

    private String generateNumber() {
        int year = Year.now().getValue();
        long seq = documentRepository.getNextSequence();
        return String.format("DOC-%d-%05d", year, seq);
    }

    private Specification<Document> buildSearchSpec(DocumentSearchRequest request) {
        return Specification
                .where(statusEquals(request.getStatus()))
                .and(authorContains(request.getAuthor()))
                .and(createdAfter(request.getCreatedFrom()))
                .and(createdBefore(request.getCreatedTo()));
    }

    private Specification<Document> statusEquals(DocumentStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    private Specification<Document> authorContains(String author) {
        return (root, query, cb) ->
                author == null ? null : cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    private Specification<Document> createdAfter(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private Specification<Document> createdBefore(LocalDateTime to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}