package org.example.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchWorkflowRequest {

    @NotEmpty(message = "Список id не может быть пустым")
    @Size(min = 1, max = 1000, message = "Список id должен содержать от 1 до 1000 элементов")
    private List<Long> ids;

    @NotBlank(message = "Инициатор не может быть пустым")
    @Size(max = 255, message = "Инициатор не может превышать 255 символов")
    private String initiator;

    @Size(max = 1000, message = "Комментарий не может превышать 1000 символов")
    private String comment; // опциональный комментарий
}