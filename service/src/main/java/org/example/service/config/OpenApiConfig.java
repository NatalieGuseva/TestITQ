package org.example.service.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document Service API")
                        .description("Сервис управления документами с workflow утверждения. " +
                                "Поддерживает создание, поиск, пакетный submit и approve документов.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ITQ Group")
                                .email("dev@itq.com")))
                .tags(List.of(
                        new Tag().name("Documents").description("Управление документами"),
                        new Tag().name("Workflow").description("Управление статусами"),
                        new Tag().name("Concurrency Test").description("Тест конкурентного утверждения")
                ));
    }
}
