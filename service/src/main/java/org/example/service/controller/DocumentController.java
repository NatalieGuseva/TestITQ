package org.example.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.service.dto.request.CreateDocumentRequest;
import org.example.service.dto.request.DocumentSearchRequest;
import org.example.service.dto.response.DocumentResponse;
import org.example.service.dto.response.DocumentWithHistoryResponse;
import org.example.service.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Управление документами")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 1) Создать документ в статусе DRAFT
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать документ", description = "Создаёт документ в статусе DRAFT. Номер генерируется автоматически.")
    public DocumentResponse createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(request);
    }

    /**
     * 2a) Получить один документ вместе с историей
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получить документ с историей", description = "Возвращает документ и полную историю его статусов.")
    public DocumentWithHistoryResponse getDocumentWithHistory(@PathVariable Long id) {
        return documentService.getDocumentWithHistory(id);
    }

    /**
     * 2b) Получить список документов по списку id (пакетное получение) с пагинацией
     */
    @GetMapping("/batch")
    @Operation(summary = "Пакетное получение документов", description = "Возвращает список документов по переданным id с пагинацией и сортировкой.")
    public Page<DocumentResponse> getDocumentsByIds(
            @RequestParam List<Long> ids,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return documentService.getDocumentsByIds(ids, pageable);
    }

    /**
     * 5) Поиск документов по фильтрам: статус, автор, период дат (по дате создания)
     */
    @GetMapping("/search")
    @Operation(summary = "Поиск документов", description = "Фильтрация по статусу, автору и периоду дат создания. Поддерживает пагинацию и сортировку.")
    public Page<DocumentResponse> searchDocuments(
            @Valid DocumentSearchRequest searchRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return documentService.searchDocuments(searchRequest, pageable);
    }
}