package org.example.service.worker;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.dto.request.BatchWorkflowRequest;
import org.example.service.dto.response.BatchWorkflowResponse;
import org.example.service.entity.Document;
import org.example.service.entity.DocumentStatus;
import org.example.service.repository.DocumentRepository;
import org.example.service.service.WorkflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApproveWorker {

    private final DocumentRepository documentRepository;
    private final WorkflowService workflowService;

    @Value("${app.worker.batch-size:50}")
    private int batchSize;

    @Value("${app.worker.initiator:approve-worker}")
    private String initiator;

    /**
     * Регулярно проверяет БД и утверждает SUBMITTED документы пачками.
     * Частичные ошибки не останавливают обработку.
     */
    @Scheduled(fixedDelayString = "${app.worker.approve.delay-ms:15000}")
    public void processApprove() {
        Page<Document> page = documentRepository.findPageByStatus(
                DocumentStatus.SUBMITTED, PageRequest.of(0, batchSize)
        );

        if (page.isEmpty()) {
            log.debug("ApproveWorker: нет документов в статусе SUBMITTED");
            return;
        }

        long total = page.getTotalElements();
        log.info("ApproveWorker: найдено {} документов в SUBMITTED, обрабатываю пачку из {}",
                total, page.getNumberOfElements());

        List<Long> ids = page.getContent().stream()
                .map(Document::getId)
                .toList();

        Instant start = Instant.now();

        BatchWorkflowRequest request = new BatchWorkflowRequest();
        request.setIds(ids);
        request.setInitiator(initiator);

        BatchWorkflowResponse response = workflowService.approveDocuments(request);

        long elapsed = Instant.now().toEpochMilli() - start.toEpochMilli();
        log.info("ApproveWorker: пачка обработана за {} мс — успешно={}, ошибок={}, осталось примерно={}",
                elapsed,
                response.getSucceeded(),
                response.getFailed(),
                total - response.getSucceeded());
    }
}
