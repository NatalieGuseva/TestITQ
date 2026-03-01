package org.example.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @NotBlank(message = "Название документа не может быть пустым")
    @Size(max = 255, message = "Название не может превышать 255 символов")
    private String title;

    @NotBlank(message = "Автор не может быть пустым")
    @Size(max = 255, message = "Автор не может превышать 255 символов")
    private String author;

    @Size(max = 255, message = "Инициатор не может превышать 255 символов")
    private String initiator;
}