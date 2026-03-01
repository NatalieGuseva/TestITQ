package org.example.service.repository;

import org.example.service.dto.request.DocumentSearchRequest;
import org.example.service.entity.Document;
import org.springframework.data.jpa.domain.Specification;

public class DocumentSpecification {

    private DocumentSpecification() {}

    /**
     * Собирает Specification из фильтров поискового запроса.
     * Все фильтры опциональны — если не заданы, не применяются.
     * Период дат трактуется по дате создания (createdAt).
     */
    public static Specification<Document> bySearchRequest(DocumentSearchRequest request) {
        return Specification
                .where(hasStatus(request))
                .and(hasAuthor(request))
                .and(createdAfter(request))
                .and(createdBefore(request));
    }

    private static Specification<Document> hasStatus(DocumentSearchRequest request) {
        return (root, query, cb) ->
                request.getStatus() == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), request.getStatus());
    }

    private static Specification<Document> hasAuthor(DocumentSearchRequest request) {
        return (root, query, cb) ->
                request.getAuthor() == null || request.getAuthor().isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("author")),
                        "%" + request.getAuthor().toLowerCase() + "%");
    }

    private static Specification<Document> createdAfter(DocumentSearchRequest request) {
        return (root, query, cb) ->
                request.getCreatedFrom() == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), request.getCreatedFrom());
    }

    private static Specification<Document> createdBefore(DocumentSearchRequest request) {
        return (root, query, cb) ->
                request.getCreatedTo() == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("createdAt"), request.getCreatedTo());
    }
}