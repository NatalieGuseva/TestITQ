package org.example.service;

import org.example.service.dto.request.BatchWorkflowRequest;
import org.example.service.dto.request.CreateDocumentRequest;
import org.example.service.dto.response.*;
import org.example.service.entity.DocumentStatus;
import org.example.service.repository.ApprovalRegistryRepository;
import org.example.service.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Happy-path: полный жизненный цикл одного документа")
class DocumentHappyPathTest extends BaseIntegrationTest {

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
    @DisplayName("Создание → Submit → Approve: статус меняется корректно, история и реестр записываются")
    void fullLifecycle() {
        // 1. Создать документ
        CreateDocumentRequest createRequest = new CreateDocumentRequest();
        createRequest.setTitle("Тестовый документ");
        createRequest.setAuthor("Иван Иванов");
        createRequest.setInitiator("test-user");

        ResponseEntity<DocumentResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/documents", createRequest, DocumentResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentResponse created = createResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getNumber()).startsWith("DOC-");
        assertThat(created.getStatus()).isEqualTo(DocumentStatus.DRAFT);

        Long docId = created.getId();

        // 2. Submit: DRAFT -> SUBMITTED
        BatchWorkflowRequest submitRequest = new BatchWorkflowRequest();
        submitRequest.setIds(List.of(docId));
        submitRequest.setInitiator("test-user");
        submitRequest.setComment("Отправляю на согласование");

        ResponseEntity<BatchWorkflowResponse> submitResponse = restTemplate.postForEntity(
                "/api/v1/documents/submit", submitRequest, BatchWorkflowResponse.class
        );

        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchWorkflowResponse submitResult = submitResponse.getBody();
        assertThat(submitResult).isNotNull();
        assertThat(submitResult.getSucceeded()).isEqualTo(1);
        assertThat(submitResult.getFailed()).isEqualTo(0);
        assertThat(submitResult.getResults().get(0).getStatus())
                .isEqualTo(DocumentOperationResult.OperationStatus.SUCCESS);

        // 3. Approve: SUBMITTED -> APPROVED
        BatchWorkflowRequest approveRequest = new BatchWorkflowRequest();
        approveRequest.setIds(List.of(docId));
        approveRequest.setInitiator("approver");
        approveRequest.setComment("Утверждаю");

        ResponseEntity<BatchWorkflowResponse> approveResponse = restTemplate.postForEntity(
                "/api/v1/documents/approve", approveRequest, BatchWorkflowResponse.class
        );

        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BatchWorkflowResponse approveResult = approveResponse.getBody();
        assertThat(approveResult).isNotNull();
        assertThat(approveResult.getSucceeded()).isEqualTo(1);
        assertThat(approveResult.getFailed()).isEqualTo(0);

        // 4. Проверить финальное состояние документа с историей
        ResponseEntity<DocumentWithHistoryResponse> historyResponse = restTemplate.getForEntity(
                "/api/v1/documents/" + docId, DocumentWithHistoryResponse.class
        );

        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentWithHistoryResponse doc = historyResponse.getBody();
        assertThat(doc).isNotNull();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(doc.getHistory()).hasSize(2); // SUBMIT + APPROVE
        assertThat(doc.getHistory().get(0).getAction().name()).isEqualTo("SUBMIT");
        assertThat(doc.getHistory().get(1).getAction().name()).isEqualTo("APPROVE");

        // 5. Проверить запись в реестре
        assertThat(approvalRegistryRepository.existsByDocumentId(docId)).isTrue();
    }

    @Test
    @DisplayName("Запрос несуществующего документа возвращает 404")
    void getNotFound() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/documents/99999", ErrorResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo("DOCUMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("Submit документа не в статусе DRAFT возвращает CONFLICT")
    void submitAlreadySubmitted() {
        // Создаём и сразу submit
        Long docId = createAndSubmit("Документ", "Автор");

        // Повторный submit
        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(List.of(docId));
        request.setInitiator("test-user");

        ResponseEntity<BatchWorkflowResponse> response = restTemplate.postForEntity(
                "/api/v1/documents/submit", request, BatchWorkflowResponse.class
        );

        assertThat(response.getBody().getResults().get(0).getStatus())
                .isEqualTo(DocumentOperationResult.OperationStatus.CONFLICT);
    }

    // --- helpers ---

    private Long createAndSubmit(String title, String author) {
        CreateDocumentRequest create = new CreateDocumentRequest();
        create.setTitle(title);
        create.setAuthor(author);
        create.setInitiator("test-user");
        Long id = restTemplate.postForEntity("/api/v1/documents", create, DocumentResponse.class)
                .getBody().getId();

        BatchWorkflowRequest submit = new BatchWorkflowRequest();
        submit.setIds(List.of(id));
        submit.setInitiator("test-user");
        restTemplate.postForEntity("/api/v1/documents/submit", submit, BatchWorkflowResponse.class);
        return id;
    }
}
