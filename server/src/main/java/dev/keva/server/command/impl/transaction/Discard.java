package dev.keva.server.command.impl.transaction;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import io.netty.channel.ChannelHandlerContext;

@Component
@CommandImpl("discard")
@ParamLength(0)
public class Discard {
    private final TransactionManager manager;

    @Autowired
    public Discard(TransactionManager manager) {
        this.manager = manager;
    }

    @Execute
    public StatusReply execute(ChannelHandlerContext ctx) {
        var txContext = manager.getTransactions().get(ctx.channel());
        if (txContext != null) {
            txContext.discard();
        }
        return new StatusReply("OK");
    }
}
