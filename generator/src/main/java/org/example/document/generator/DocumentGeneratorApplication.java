package org.example.document.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentGeneratorApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(DocumentGeneratorApplication.class, args)));
    }
}