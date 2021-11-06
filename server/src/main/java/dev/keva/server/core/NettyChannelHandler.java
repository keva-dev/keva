package dev.keva.server.core;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.ioc.annotation.Qualifier;
import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import dev.keva.server.command.mapping.CommandMapper;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Sharable
@Component
public class NettyChannelHandler extends SimpleChannelInboundHandler<Command> {
    private static final byte LOWER_DIFF = 'a' - 'A';
    private final CommandMapper commandMapper;
    private final TransactionManager transactionManager;

    @Autowired
    public NettyChannelHandler(CommandMapper commandMapper, TransactionManager transactionManager) {
        this.commandMapper = commandMapper;
        this.transactionManager = transactionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) throws InterruptedException {
        val lock = transactionManager.getTransactionLock();
        var isLocked = lock.isLocked();
        var pollingTime = 10;
        while (isLocked) {
            Thread.sleep(pollingTime);
            pollingTime = pollingTime < 100 ? pollingTime + 10 : pollingTime;
            isLocked = lock.isLocked();
        }

        val name = command.getName();
        // LowerCase bytes
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            if (b >= 'A' && b <= 'Z') {
                name[i] = (byte) (b + LOWER_DIFF);
            }
        }
        val commandWrapper = commandMapper.getMethods().get(new BytesKey(name));
        Reply<?> reply;
        if (commandWrapper == null) {
            reply = new ErrorReply("ERR unknown command `" + new String(name) + "`");
        } else {
            reply = commandWrapper.execute(ctx, command);
        }
        if (reply != null) {
            ctx.write(reply);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Handler exception caught: ", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("IdleStateEvent triggered, close channel " + ctx.channel());
            channelUnregistered(ctx);
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
