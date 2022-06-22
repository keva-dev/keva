package dev.keva.core.command.impl.transaction;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.transaction.manager.TransactionContext;
import dev.keva.core.command.impl.transaction.manager.TransactionManager;
import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;

@Component
@CommandImpl("multi")
@ParamLength(0)
public class Multi {
    private final KevaDatabase database;
    private final TransactionManager manager;
    private final CommandMapper commandMapper;

    @Autowired
    public Multi(KevaDatabase database, TransactionManager manager, CommandMapper commandMapper) {
        this.database = database;
        this.manager = manager;
        this.commandMapper = commandMapper;
    }

    @Execute
    public StatusReply execute(ChannelHandlerContext ctx) {
        TransactionContext txContext = manager.getTransactions().get(ctx.channel());
        if (txContext == null) {
            txContext = new TransactionContext(database, commandMapper);
            manager.getTransactions().put(ctx.channel(), txContext);
        }
        txContext.multi();
        return StatusReply.OK;
    }
}
