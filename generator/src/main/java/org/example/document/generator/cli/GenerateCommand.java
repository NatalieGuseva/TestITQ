package org.example.document.generator.cli;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.document.generator.service.DocumentGeneratorService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Component
@RequiredArgsConstructor
@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Генерирует N документов через API сервиса"
)
public class GenerateCommand implements Runnable {

    private final DocumentGeneratorService generatorService;

    @Option(
            names = {"-n", "--count"},
            description = "Количество документов (по умолчанию берётся из конфига)",
            defaultValue = "0"
    )
    private int count;

    @Option(
            names = {"-a", "--author"},
            description = "Автор документов",
            defaultValue = "generator-bot"
    )
    private String author;

    @Override
    public void run() {
        log.info("CLI запущен: count={}, author={}", count, author);
        generatorService.generate(count, author);
    }
}
