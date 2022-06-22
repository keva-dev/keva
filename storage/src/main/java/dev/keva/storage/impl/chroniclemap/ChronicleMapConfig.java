package dev.keva.storage.impl.chroniclemap;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChronicleMapConfig {
    Boolean isPersistence;
    String workingDirectory;
}
