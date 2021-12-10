package dev.keva.server.command.impl.transaction;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

@Component
@CommandImpl("unwatch")
public class Unwatch {
    private final TransactionManager manager;

    @Autowired
    public Unwatch(TransactionManager manager) {
        this.manager = manager;
    }

    @Execute
    public StatusReply execute(ChannelHandlerContext ctx, byte[]... keys) {
        var txContext = manager.getTransactions().get(ctx.channel());
        if (txContext != null) {
            if (keys.length == 0) {
                txContext.getWatchMap().clear();
            } else {
                for (val key : keys) {
                    txContext.getWatchMap().remove(new BytesKey(key));
                }
            }
        }
        return StatusReply.OK;
    }
}
