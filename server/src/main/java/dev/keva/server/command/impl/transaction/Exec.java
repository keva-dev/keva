package dev.keva.server.command.impl.transaction;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import io.netty.channel.ChannelHandlerContext;

import static dev.keva.protocol.resp.reply.BulkReply.NIL_REPLY;

@Component
@CommandImpl("exec")
@ParamLength(0)
public class Exec {
    private final TransactionManager manager;

    @Autowired
    public Exec(TransactionManager manager) {
        this.manager = manager;
    }

    @Execute
    public Reply<?> execute(ChannelHandlerContext ctx) throws InterruptedException {
        var context = manager.getTransactions().get(ctx.channel());
        if (context == null) {
            return NIL_REPLY;
        }
        return context.exec(ctx, manager.getTransactionLock());
    }
}
