package dev.keva.server.command;

import dev.keva.ioc.annotation.Component;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.protocol.resp.reply.StatusReply;

@Component
@CommandImpl("quit")
@ParamLength(0)
public class Quit {
    @Execute
    public StatusReply execute() {
        return new StatusReply("OK");
    }
}
