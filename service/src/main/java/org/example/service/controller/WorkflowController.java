package org.example.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.service.dto.request.BatchWorkflowRequest;
import org.example.service.dto.response.BatchWorkflowResponse;
import org.example.service.service.WorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "Управление статусами документов")
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 3) Отправить на согласование: DRAFT -> SUBMITTED
     * Принимает список id (1-1000), для каждого возвращает: успешно / конфликт / не найдено
     */
    @PostMapping("/submit")
    @Operation(
            summary = "Отправить документы на согласование",
            description = "Переводит документы из статуса DRAFT в SUBMITTED. " +
                    "Принимает от 1 до 1000 id. Обработка каждого документа атомарна. " +
                    "Результат по каждому id: SUCCESS / CONFLICT / NOT_FOUND."
    )
    public BatchWorkflowResponse submitDocuments(@Valid @RequestBody BatchWorkflowRequest request) {
        return workflowService.submitDocuments(request);
    }

    /**
     * 4) Утвердить: SUBMITTED -> APPROVED
     * Принимает список id (1-1000), для каждого возвращает: успешно / конфликт / не найдено / ошибка регистрации
     * При успехе: пишется запись в историю + создаётся запись в реестре утверждений
     * Если реестр не создан — утверждение откатывается
     */
    @PostMapping("/approve")
    @Operation(
            summary = "Утвердить документы",
            description = "Переводит документы из статуса SUBMITTED в APPROVED. " +
                    "Принимает от 1 до 1000 id. Обработка каждого документа атомарна. " +
                    "Результат по каждому id: SUCCESS / CONFLICT / NOT_FOUND / REGISTRY_ERROR. " +
                    "При ошибке записи в реестр — утверждение документа откатывается."
    )
    public BatchWorkflowResponse approveDocuments(@Valid @RequestBody BatchWorkflowRequest request) {
        return workflowService.approveDocuments(request);
    }
}