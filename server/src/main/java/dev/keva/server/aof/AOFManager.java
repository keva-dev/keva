package dev.keva.server.aof;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.server.command.mapping.CommandMapper;
import dev.keva.server.config.KevaConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class AOFManager {
    private final KevaConfig kevaConfig;
    private final CommandMapper commandMapper;
    private final AOFContainer aofOperations;

    @Autowired
    public AOFManager(KevaConfig kevaConfig, CommandMapper commandMapper, AOFContainer aofOperations) {
        this.kevaConfig = kevaConfig;
        this.commandMapper = commandMapper;
        this.aofOperations = aofOperations;
    }

    public void init() {
        if (!kevaConfig.getAof()) {
            return;
        }

        try {
            List<Command> commands = aofOperations.read();
            if (commands != null) {
                log.info("Loading AOF file");
                for (Command command : commands) {
                    val name = command.getName();
                    val commandWrapper = commandMapper.getMethods().get(new BytesKey(name));
                    if (commandWrapper != null) {
                        commandWrapper.execute(null, command);
                    }
                }
                log.info("Recovered {} commands from AOF file", commands.size());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Cannot read AOF file", e);
        }

        aofOperations.init();
    }
}
