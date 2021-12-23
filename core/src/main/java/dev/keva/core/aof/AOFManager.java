package dev.keva.core.aof;

import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.core.command.mapping.CommandWrapper;
import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.util.hashbytes.BytesKey;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class AOFManager {
    private final KevaConfig kevaConfig;
    private final CommandMapper commandMapper;
    private final AOFContainer aof;

    @Autowired
    public AOFManager(KevaConfig kevaConfig, CommandMapper commandMapper, AOFContainer aof) {
        this.kevaConfig = kevaConfig;
        this.commandMapper = commandMapper;
        this.aof = aof;
    }

    public void init() {
        if (!kevaConfig.getAof()) {
            return;
        }

        try {
            List<Command> commands = aof.read();
            if (commands != null) {
                log.info("Processing {} commands from AOF file", commands.size());
                for (Command command : commands) {
                    byte[] name = command.getName();
                    CommandWrapper commandWrapper = commandMapper.getMethods().get(new BytesKey(name));
                    if (commandWrapper != null) {
                        commandWrapper.execute(null, command);
                    }
                }
                log.info("Recovered {} commands from AOF file", commands.size());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Cannot read AOF file", e);
        }

        aof.init();
    }
}
