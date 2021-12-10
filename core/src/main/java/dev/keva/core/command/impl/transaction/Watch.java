package dev.keva.core.command.impl.transaction;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.transaction.manager.TransactionContext;
import dev.keva.core.command.impl.transaction.manager.TransactionManager;
import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.store.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("watch")
@ParamLength(type = AT_LEAST, value = 1)
public class Watch {
    private final KevaDatabase database;
    private final TransactionManager manager;
    private final CommandMapper commandMapper;

    @Autowired
    public Watch(KevaDatabase database, TransactionManager manager, CommandMapper commandMapper) {
        this.database = database;
        this.manager = manager;
        this.commandMapper = commandMapper;
    }

    @Execute
    public StatusReply execute(ChannelHandlerContext ctx, byte[]... keys) {
        var txContext = manager.getTransactions().get(ctx.channel());
        if (txContext == null) {
            txContext = new TransactionContext(database, commandMapper);
            manager.getTransactions().put(ctx.channel(), txContext);
        }
        for (val key : keys) {
            val value = database.get(key);
            txContext.getWatchMap().put(new BytesKey(key), new BytesValue(value));
        }
        return StatusReply.OK;
    }
}
