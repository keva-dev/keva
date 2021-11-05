package dev.keva.server.command.impl.transaction.manager;

import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.hashbytes.BytesValue;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.mapping.CommandMapper;
import dev.keva.store.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.val;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static dev.keva.protocol.resp.reply.BulkReply.NIL_REPLY;

public class TransactionContext {
    private final KevaDatabase database;
    private final CommandMapper commandMapper;

    @Getter
    private boolean isQueuing = false;
    @Getter
    private final Map<BytesKey, BytesValue> watchMap = new HashMap<>();
    @Getter
    private final Deque<Command> commandDeque = new ArrayDeque<>();

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

    public Reply<?> exec(ChannelHandlerContext ctx, ReentrantLock txLock) throws InterruptedException {
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
            Reply<?>[] replies = new Reply[commandDeque.size()];
            var i = 0;
            while(commandDeque.size() > 0) {
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
