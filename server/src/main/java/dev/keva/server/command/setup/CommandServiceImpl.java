package dev.keva.server.command.setup;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import dev.keva.server.protocol.resp.Command;
import dev.keva.server.protocol.resp.reply.ErrorReply;
import dev.keva.server.protocol.resp.reply.Reply;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
public class CommandServiceImpl implements CommandService {

    private final Injector injector;

    public CommandServiceImpl(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Reply<?> handleCommand(String name, Command command) {
        Reply<?> output;
        try {
            val handler = getCommandHandler(name);
            var objects = command.getObjects();

            if (command.isInline()) {
                val bytes = command.getName();
                var msgStr = new String(bytes, StandardCharsets.UTF_8);
                var msgArr = msgStr.trim().split("\\s+");
                objects = Arrays.asList(msgArr);
            }

            output = handler.handle(objects);
        } catch (ConfigurationException | ProvisionException e) {
            return new ErrorReply("ERR unknown command `" + name.toUpperCase() + "`");
        } catch (Exception e) {
            log.error("Error while handling command: ", e);
            output = new ErrorReply("ERR unknown error: " + e.getMessage());
        }
        return output;
    }

    private CommandHandler getCommandHandler(String name) {
        return injector.getInstance(Key
                .get(CommandHandler.class, Names.named(name.toUpperCase())));
    }
}
