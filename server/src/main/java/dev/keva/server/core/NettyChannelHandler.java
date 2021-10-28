package dev.keva.server.core;

import dev.keva.server.command.wrapper.CommandWrapper;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.protocol.resp.Command;
import dev.keva.server.protocol.resp.hashbytes.BytesKey;
import dev.keva.server.protocol.resp.reply.ErrorReply;
import dev.keva.server.protocol.resp.reply.Reply;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.reflections.Reflections;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Sharable
public class NettyChannelHandler extends SimpleChannelInboundHandler<Command> {
    private final ConcurrentMap<BytesKey, CommandWrapper> methods = new ConcurrentHashMap<>();

    public NettyChannelHandler() {
        Reflections reflections = new Reflections("dev.keva.server.command");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(dev.keva.server.command.annotation.Command.class);
        for (Class<?> aClass : annotated) {
            for (val method : aClass.getMethods()) {
                if (method.isAnnotationPresent(Execute.class)) {
                    val types = method.getParameterTypes();
                    val commandName = aClass.getAnnotation(dev.keva.server.command.annotation.Command.class).value();
                    methods.put(new BytesKey(commandName.getBytes()), command -> {
                        try {
                            Object[] objects = new Object[types.length];
                            command.toArguments(objects, types);
                            return (Reply<?>) method.invoke(null, objects);
                        } catch (Exception e) {
                            log.error("", e);
                            return null;
                        }
                    });
                }
            }
        }
    }

    private static final byte LOWER_DIFF = 'a' - 'A';

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) {
        val name = msg.getName();
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            if (b >= 'A' && b <= 'Z') {
                name[i] = (byte) (b + LOWER_DIFF);
            }
        }
        CommandWrapper wrapper = methods.get(new BytesKey(name));
        Reply<?> reply;
        if (wrapper == null) {
            reply = new ErrorReply("ERR unknown command '" + new String(name).toUpperCase() + "'");
        } else {
            reply = wrapper.execute(msg);
        }
        ctx.write(reply);
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
