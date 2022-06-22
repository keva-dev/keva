package dev.keva.core.command.impl.server;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.core.config.KevaConfig;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("config")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 1)
public class Config {
    private final KevaConfig config;
    private final CommandMapper commandMapper;

    @Autowired
    public Config(KevaConfig config, CommandMapper commandMapper) {
        this.config = config;
        this.commandMapper = commandMapper;
    }

    @Execute
    public Reply<?> execute(byte[] command, byte[] key, byte[] value) {
        String commandStr = new String(command, StandardCharsets.UTF_8);
        if (commandStr.equalsIgnoreCase("set")) {
            String keyStr = new String(key, StandardCharsets.UTF_8);
            if (value == null) {
                throw new CommandException("Value is required for " + keyStr + " command");
            }
            String valueStr = new String(value, StandardCharsets.UTF_8);
            if (keyStr.equals("requirepass")) {
                config.setPassword(valueStr);
                commandMapper.init();
                return StatusReply.OK;
            } else {
                throw new CommandException("Unknown config key: " + keyStr);
            }
        } else if (commandStr.equalsIgnoreCase("get")) {
            String keyStr = new String(key, StandardCharsets.UTF_8);
            if (keyStr.equals("requirepass")) {
                return new BulkReply(config.getPassword());
            } else {
                throw new CommandException("Unknown config key: " + keyStr);
            }
        }

        throw new CommandException("Unknown config command: " + commandStr);
    }
}
