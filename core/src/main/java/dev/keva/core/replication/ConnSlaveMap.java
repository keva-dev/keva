package dev.keva.core.replication;

import dev.keva.ioc.annotation.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ConnSlaveMap {
    private static final Map<String, SlaveContext> connSlaveMap = new HashMap<>();

    public void put(String key, SlaveContext ctx) {
        connSlaveMap.put(key, ctx);
    }

    public SlaveContext get(String key) {
        return connSlaveMap.get(key);
    }

    public boolean contains(String key) {
        return connSlaveMap.containsKey(key);
    }
}
