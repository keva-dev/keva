package dev.keva.server.command.impl.transaction;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import static dev.keva.server.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("unwatch")
@ParamLength(type = AT_LEAST, value = 1)
public class Unwatch {
    private final TransactionManager manager;

    @Autowired
    public Unwatch(TransactionManager manager) {
        this.manager = manager;
    }

    @Execute
    public StatusReply execute(ChannelHandlerContext ctx, byte[]... keys) {
        var context = manager.getTransactions().get(ctx.channel());
        if (context != null) {
            for (val key : keys) {
                context.getWatchMap().remove(new BytesKey(key));
            }
        }
        return new StatusReply("OK");
    }
}
