package dev.keva.server.command.impl.kql.manager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public class KevaTable implements Serializable {
    @Getter
    private final List<KevaColumnDefinition> columns;

    @Getter
    private long increment = 1;

    public void increment() {
        increment++;
    }
}
