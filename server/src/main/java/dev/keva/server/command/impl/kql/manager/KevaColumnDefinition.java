package dev.keva.server.command.impl.kql.manager;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
public class KevaColumnDefinition implements Serializable {
    public String name;
    public String type;
}
