package dev.keva.core.server;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
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
        Timer timer = SharedMetricRegistries.getDefault().timer(MetricRegistry.name(NettyChannelHandler.class, "channelRead0"));
        Timer.Context context = timer.time();
        val name = command.getName();
        Timer timer2 = SharedMetricRegistries.getDefault().timer(MetricRegistry.name(NettyChannelHandler.class, "getCommand"));
        Timer.Context context2 = timer2.time();
        val commandWrapper = commandMapper.getMethods().get(new BytesKey(name));
        context2.stop();
        Reply<?> reply;
        if (commandWrapper == null) {
            reply = new ErrorReply("ERR unknown command `" + new String(name) + "`");
        } else {
            Timer timer4 = SharedMetricRegistries.getDefault().timer(MetricRegistry.name(NettyChannelHandler.class, "commandExecute"));
            Timer.Context context4 = timer4.time();
            reply = commandWrapper.execute(ctx, command);
            context4.stop();
        }
        Timer timer3 = SharedMetricRegistries.getDefault().timer(MetricRegistry.name(NettyChannelHandler.class, "replyWrite"));
        Timer.Context context3 = timer3.time();
        if (reply != null) {
            ctx.write(reply);
            context3.stop();
        }
        context.stop();
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
