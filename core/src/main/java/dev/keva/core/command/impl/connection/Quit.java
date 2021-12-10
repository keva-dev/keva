package dev.keva.core.command.impl.connection;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;

@Component
@CommandImpl("quit")
@ParamLength(0)
public class Quit {
    @Execute
    public StatusReply execute() {
        return StatusReply.OK;
    }
}
