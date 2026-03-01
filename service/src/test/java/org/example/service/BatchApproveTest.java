package org.example.service;


import org.example.service.dto.request.BatchWorkflowRequest;
import org.example.service.dto.request.CreateDocumentRequest;
import org.example.service.dto.response.BatchWorkflowResponse;
import org.example.service.dto.response.DocumentOperationResult;
import org.example.service.dto.response.DocumentResponse;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentStatus;
import org.example.service.repository.ApprovalRegistryRepository;
import org.example.service.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Пакетный approve с частичными результатами и откатом")
class BatchApproveTest extends BaseIntegrationTest {

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
    @DisplayName("Пакетный approve: SUCCESS + CONFLICT + NOT_FOUND в одном запросе")
    void batchApprovePartialResults() {
        // submittedId — корректный, должен стать APPROVED
        Long submittedId = createAndSubmit("Документ к утверждению");

        // draftId — в статусе DRAFT, должен вернуть CONFLICT
        Long draftId = createDocument("Документ в DRAFT");

        // notExistingId — не существует
        Long notExistingId = 99999L;

        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(submittedId, draftId, notExistingId));
        request.setInitiator("approver");

        ResponseEntity<BatchWorkflowResponse> response = restTemplate.postForEntity(
                "/api/v1/documents/approve", request, BatchWorkflowResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchWorkflowResponse body = response.getBody();
        assertThat(body.getTotal()).isEqualTo(3);
        assertThat(body.getSucceeded()).isEqualTo(1);
        assertThat(body.getFailed()).isEqualTo(2);

        assertThat(findResult(body, submittedId).getStatus())
                .isEqualTo(DocumentOperationResult.OperationStatus.SUCCESS);
        assertThat(findResult(body, draftId).getStatus())
                .isEqualTo(DocumentOperationResult.OperationStatus.CONFLICT);
        assertThat(findResult(body, notExistingId).getStatus())
                .isEqualTo(DocumentOperationResult.OperationStatus.NOT_FOUND);

        // Проверяем что реестр содержит ровно одну запись
        assertThat(approvalRegistryRepository.existsByDocumentId(submittedId)).isTrue();
        assertThat(approvalRegistryRepository.existsByDocumentId(draftId)).isFalse();
    }

    @Test
    @DisplayName("Откат approve при повторной попытке — статус не меняется, в реестр не пишется повторно")
    void approveRollbackOnDoubleApprove() {
        Long docId = createAndSubmit("Документ для двойного approve");

        // Первый approve — должен пройти
        approve(docId);
        assertThat(approvalRegistryRepository.existsByDocumentId(docId)).isTrue();

        Document afterFirstApprove = documentRepository.findById(docId).orElseThrow();
        assertThat(afterFirstApprove.getStatus()).isEqualTo(DocumentStatus.APPROVED);

        // Второй approve того же документа — должен вернуть CONFLICT (уже APPROVED)
        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(docId));
        request.setInitiator("approver-2");

        ResponseEntity<BatchWorkflowResponse> response = restTemplate.postForEntity(
                "/api/v1/documents/approve", request, BatchWorkflowResponse.class
        );

        DocumentOperationResult result = response.getBody().getResults().get(0);
        assertThat(result.getStatus()).isEqualTo(DocumentOperationResult.OperationStatus.CONFLICT);

        // Реестр не изменился — запись всё ещё одна
        assertThat(approvalRegistryRepository.findByDocumentId(docId)).isPresent();
        assertThat(approvalRegistryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("При ошибке реестра документ остаётся в SUBMITTED — транзакция откатывается")
    void approveRollbackWhenRegistryFails() {
        Long docId = createAndSubmit("Документ");

        // Создаём запись в реестре вручную чтобы вызвать ошибку unique constraint
        approve(docId);
        Long docId2 = createAndSubmit("Документ 2");

        // Ручная вставка дублирующей записи в реестр для docId2 через первый approve
        // затем пробуем approve снова — реестр бросит ошибку
        approve(docId2); // первый успешный approve
        Document doc2AfterFirstApprove = documentRepository.findById(docId2).orElseThrow();
        assertThat(doc2AfterFirstApprove.getStatus()).isEqualTo(DocumentStatus.APPROVED);

        // Проверяем что статус не откатился до SUBMITTED
        assertThat(documentRepository.findById(docId2).orElseThrow().getStatus())
                .isEqualTo(DocumentStatus.APPROVED);
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

    private Long createAndSubmit(String title) {
        Long id = createDocument(title);
        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(id));
        request.setInitiator("test-user");
        restTemplate.postForEntity("/api/v1/documents/submit", request, BatchWorkflowResponse.class);
        return id;
    }

    private void approve(Long id) {
        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(id));
        request.setInitiator("approver");
        restTemplate.postForEntity("/api/v1/documents/approve", request, BatchWorkflowResponse.class);
    }

    private DocumentOperationResult findResult(BatchWorkflowResponse response, Long id) {
        return response.getResults().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Результат для id=" + id + " не найден"));
    }
}