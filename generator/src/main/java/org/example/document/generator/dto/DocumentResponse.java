package org.example.document.generator.dto;

import lombok.Data;

@Data
public class DocumentResponse {

    private Long id;
    private String number;
    private String title;
    private String author;
    private String status;
}
