package org.example.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchWorkflowResponse {

    private int total;      // всего документов в запросе
    private int succeeded;  // успешно обработано
    private int failed;     // с ошибками

    private List<DocumentOperationResult> results; // детальный результат по каждому id
}