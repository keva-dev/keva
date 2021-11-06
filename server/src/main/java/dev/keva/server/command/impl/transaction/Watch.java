package dev.keva.server.command.impl.transaction;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.transaction.manager.TransactionContext;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import dev.keva.server.command.mapping.CommandMapper;
import dev.keva.store.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import static dev.keva.server.command.annotation.ParamLength.Type.AT_LEAST;

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
        var context = manager.getTransactions().get(ctx.channel());
        if (context == null) {
            context = new TransactionContext(database, commandMapper);
            manager.getTransactions().put(ctx.channel(), context);
        }
        for (val key : keys) {
            val value = database.get(key);
            context.getWatchMap().put(new BytesKey(key), new BytesValue(value));
        }
        return new StatusReply("OK");
    }
}
