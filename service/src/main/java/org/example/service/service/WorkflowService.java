package org.example.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.dto.request.BatchWorkflowRequest;
import org.example.service.dto.response.BatchWorkflowResponse;
import org.example.service.dto.response.DocumentOperationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    // Вынесены в отдельный бин чтобы @Transactional работал через Spring proxy
    private final WorkflowTransactionService workflowTransactionService;

    public BatchWorkflowResponse submitDocuments(BatchWorkflowRequest request) {
        List<DocumentOperationResult> results = new ArrayList<>();
        for (Long id : request.getIds()) {
            results.add(workflowTransactionService.submitOne(
                    id, request.getInitiator(), request.getComment()
            ));
        }
        return buildBatchResponse(results);
    }

    public BatchWorkflowResponse approveDocuments(BatchWorkflowRequest request) {
        List<DocumentOperationResult> results = new ArrayList<>();
        for (Long id : request.getIds()) {
            results.add(workflowTransactionService.approveOne(
                    id, request.getInitiator(), request.getComment()
            ));
        }
        return buildBatchResponse(results);
    }

    private BatchWorkflowResponse buildBatchResponse(List<DocumentOperationResult> results) {
        long succeeded = results.stream()
                .filter(r -> r.getStatus() == DocumentOperationResult.OperationStatus.SUCCESS)
                .count();
        return BatchWorkflowResponse.builder()
                .total(results.size())
                .succeeded((int) succeeded)
                .failed((int) (results.size() - succeeded))
                .results(results)
                .build();
    }
}