package dev.keva.server.command;

import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import static dev.keva.server.command.annotation.ParamLength.Type.AT_LEAST;
import dev.keva.server.command.base.BaseCommandImpl;
import dev.keva.protocol.resp.reply.IntegerReply;

@CommandImpl("del")
@ParamLength(type = AT_LEAST, value = 1)
public class Del extends BaseCommandImpl {
    @Execute
    public IntegerReply execute(byte[]... keys) {
        var deleted = 0;
        for (byte[] key : keys) {
            if (database.remove(key)) {
                deleted++;
            }
        }
        return new IntegerReply(deleted);
    }
}
