package dev.keva.core.command.impl.transaction.manager;

import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.protocol.resp.Command;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.util.hashbytes.BytesValue;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.store.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.val;
import lombok.var;

import java.util.*;
import java.util.concurrent.locks.Lock;

import static dev.keva.protocol.resp.reply.BulkReply.NIL_REPLY;

public class TransactionContext {
    private final KevaDatabase database;
    private final CommandMapper commandMapper;
    @Getter
    private final Map<BytesKey, BytesValue> watchMap = new HashMap<>();
    @Getter
    private final Deque<Command> commandDeque = new ArrayDeque<>();
    @Getter
    private boolean isQueuing = false;

    public TransactionContext(KevaDatabase database, CommandMapper commandMapper) {
        this.database = database;
        this.commandMapper = commandMapper;
    }

    public void multi() {
        isQueuing = true;
    }

    public void discard() {
        commandDeque.clear();
        watchMap.clear();
        isQueuing = false;
    }

    public Reply<?> exec(ChannelHandlerContext ctx, Lock txLock) throws InterruptedException {
        txLock.lock();
        try {
            for (val watch : watchMap.entrySet()) {
                val key = watch.getKey();
                val value = watch.getValue().getBytes();
                val actualValue = database.get(key.getBytes());
                if (!Arrays.equals(actualValue, value)) {
                    discard();
                    return NIL_REPLY;
                }
            }

            isQueuing = false;
            val replies = new Reply[commandDeque.size()];
            var i = 0;
            while (commandDeque.size() > 0) {
                val command = commandDeque.removeFirst();
                val commandWrapper = commandMapper.getMethods().get(new BytesKey(command.getName()));
                if (commandWrapper == null) {
                    return NIL_REPLY;
                } else {
                    val result = commandWrapper.execute(ctx, command);
                    replies[i] = result;
                }
                i++;
            }

            watchMap.clear();

            return new MultiBulkReply(replies);
        } finally {
            txLock.unlock();
        }
    }
}
