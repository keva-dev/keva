package dev.keva.core.command.impl.replication;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("replconf")
@ParamLength(type = AT_LEAST, value = 2)
@Mutate
public class Replconf {
    @Execute
    public StatusReply execute(byte[]... args) {
        return StatusReply.OK;
    }
}
