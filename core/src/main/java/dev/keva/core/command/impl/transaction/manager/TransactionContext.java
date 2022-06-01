package dev.keva.core.command.impl.transaction.manager;

import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.core.command.mapping.CommandWrapper;
import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.util.hashbytes.BytesValue;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

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
            for (Map.Entry<BytesKey, BytesValue> watch : watchMap.entrySet()) {
                BytesKey key = watch.getKey();
                byte[] value = watch.getValue().getBytes();
                byte[] actualValue = database.get(key.getBytes());
                if (!Arrays.equals(actualValue, value)) {
                    discard();
                    return NIL_REPLY;
                }
            }

            isQueuing = false;
            Reply<?>[] replies = new Reply[commandDeque.size()];
            int i = 0;
            while (commandDeque.size() > 0) {
                Command command = commandDeque.removeFirst();
                CommandWrapper commandWrapper = commandMapper.getMethods().get(new BytesKey(command.getName()));
                if (commandWrapper == null) {
                    return NIL_REPLY;
                } else {
                    Reply<?> result = commandWrapper.execute(ctx, command);
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
