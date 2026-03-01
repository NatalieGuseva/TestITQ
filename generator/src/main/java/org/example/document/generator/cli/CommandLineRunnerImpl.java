package org.example.document.generator.cli;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@Component
@RequiredArgsConstructor
public class CommandLineRunnerImpl implements CommandLineRunner {

    private final GenerateCommand generateCommand;
    private final IFactory factory; // picocli-spring-boot-starter предоставляет этот бин

    @Override
    public void run(String... args) {
        int exitCode = new CommandLine(generateCommand, factory).execute(args);
        System.exit(exitCode);
    }
}
