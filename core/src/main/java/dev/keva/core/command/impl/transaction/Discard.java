package dev.keva.core.command.impl.transaction;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.transaction.manager.TransactionManager;
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
        return StatusReply.OK;
    }
}
