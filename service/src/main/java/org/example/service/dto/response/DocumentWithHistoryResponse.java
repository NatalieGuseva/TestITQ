package org.example.service.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentWithHistoryResponse extends DocumentResponse {

    private List<DocumentHistoryResponse> history;
}
