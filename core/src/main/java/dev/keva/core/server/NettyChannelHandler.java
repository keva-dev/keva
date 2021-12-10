package dev.keva.core.server;

import dev.keva.core.command.mapping.CommandMapper;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@Sharable
@Component
public class NettyChannelHandler extends SimpleChannelInboundHandler<Command> {
    private final CommandMapper commandMapper;

    @Autowired
    public NettyChannelHandler(CommandMapper commandMapper) {
        this.commandMapper = commandMapper;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) throws InterruptedException {
        val name = command.getName();
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
        if (!cause.getMessage().equals("Connection reset by peer")) {
            log.error("Handler exception caught: ", cause);
        }
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
