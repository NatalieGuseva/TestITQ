package org.example.service;


import org.example.service.dto.request.BatchWorkflowRequest;
import org.example.service.dto.request.CreateDocumentRequest;
import org.example.service.dto.response.BatchWorkflowResponse;
import org.example.service.dto.response.DocumentOperationResult;
import org.example.service.dto.response.DocumentResponse;
import org.example.service.repository.ApprovalRegistryRepository;
import org.example.service.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Пакетный submit с частичными результатами")
class BatchSubmitTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApprovalRegistryRepository approvalRegistryRepository;

    @BeforeEach
    void cleanUp() {
        approvalRegistryRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    @DisplayName("Пакетный submit: SUCCESS + CONFLICT + NOT_FOUND в одном запросе")
    void batchSubmitPartialResults() {
        // Создаём 2 документа
        Long draftId1 = createDocument("Документ 1");
        Long draftId2 = createDocument("Документ 2");

        // Переводим второй сразу в SUBMITTED чтобы получить CONFLICT
        submitDocument(draftId2);

        // Несуществующий id
        Long notExistingId = 99999L;

        // Пакетный submit: draftId1 (DRAFT -> SUCCESS), draftId2 (SUBMITTED -> CONFLICT), notExistingId (NOT_FOUND)
        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(draftId1, draftId2, notExistingId));
        request.setInitiator("batch-tester");

        ResponseEntity<BatchWorkflowResponse> response = restTemplate.postForEntity(
                "/api/v1/documents/submit", request, BatchWorkflowResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchWorkflowResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTotal()).isEqualTo(3);
        assertThat(body.getSucceeded()).isEqualTo(1);
        assertThat(body.getFailed()).isEqualTo(2);

        // Проверяем результаты по каждому id
        DocumentOperationResult result1 = findResult(body, draftId1);
        DocumentOperationResult result2 = findResult(body, draftId2);
        DocumentOperationResult result3 = findResult(body, notExistingId);

        assertThat(result1.getStatus()).isEqualTo(DocumentOperationResult.OperationStatus.SUCCESS);
        assertThat(result2.getStatus()).isEqualTo(DocumentOperationResult.OperationStatus.CONFLICT);
        assertThat(result3.getStatus()).isEqualTo(DocumentOperationResult.OperationStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Пакетный submit 100 документов — все должны стать SUBMITTED")
    void batchSubmitLarge() {
        int count = 100;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(createDocument("Документ-" + i));
        }

        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(ids);
        request.setInitiator("bulk-tester");

        ResponseEntity<BatchWorkflowResponse> response = restTemplate.postForEntity(
                "/api/v1/documents/submit", request, BatchWorkflowResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getSucceeded()).isEqualTo(count);
        assertThat(response.getBody().getFailed()).isEqualTo(0);
    }

    // --- helpers ---

    private Long createDocument(String title) {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle(title);
        request.setAuthor("Автор");
        request.setInitiator("test-user");
        return restTemplate.postForEntity("/api/v1/documents", request, DocumentResponse.class)
                .getBody().getId();
    }

    private void submitDocument(Long id) {
        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(id));
        request.setInitiator("test-user");
        restTemplate.postForEntity("/api/v1/documents/submit", request, BatchWorkflowResponse.class);
    }

    private DocumentOperationResult findResult(BatchWorkflowResponse response, Long id) {
        return response.getResults().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Результат для id=" + id + " не найден"));
    }
}
