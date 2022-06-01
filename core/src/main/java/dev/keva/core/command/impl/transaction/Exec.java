package dev.keva.core.command.impl.transaction;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.transaction.manager.TransactionContext;
import dev.keva.core.command.impl.transaction.manager.TransactionManager;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;

import static dev.keva.protocol.resp.reply.BulkReply.NIL_REPLY;

@Component
@CommandImpl("exec")
@ParamLength(0)
public class Exec {
    private final TransactionManager manager;
    private final KevaDatabase database;

    @Autowired
    public Exec(TransactionManager manager, KevaDatabase database) {
        this.manager = manager;
        this.database = database;
    }

    @Execute
    public Reply<?> execute(ChannelHandlerContext ctx) throws InterruptedException {
        TransactionContext txContext = manager.getTransactions().get(ctx.channel());
        if (txContext == null) {
            return NIL_REPLY;
        }
        return txContext.exec(ctx, database.getLock());
    }
}
