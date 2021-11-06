package dev.keva.store;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatabaseConfig {
    Boolean isPersistence;
    String workingDirectory;
}
