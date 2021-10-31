package dev.keva.server.core;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.wrapper.CommandWrapper;
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
@Component
public class NettyChannelHandler extends SimpleChannelInboundHandler<Command> {

    private final ConcurrentMap<BytesKey, CommandWrapper> methods = new ConcurrentHashMap<>();

    @Autowired
    public NettyChannelHandler(KevaIoC context) {
        Reflections reflections = new Reflections("dev.keva.server.command");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(CommandImpl.class);
        for (Class<?> aClass : annotated) {
            for (val method : aClass.getMethods()) {
                if (method.isAnnotationPresent(Execute.class)) {
                    val types = method.getParameterTypes();
                    val name = aClass.getAnnotation(CommandImpl.class).value();
                    val paramLength = aClass.getAnnotation(ParamLength.class) != null ? aClass.getAnnotation(ParamLength.class).value() : -1;
                    methods.put(new BytesKey(name.getBytes()), (ctx, command) -> {
                        if (paramLength != -1) {
                            if (aClass.getAnnotation(ParamLength.class).type() == ParamLength.Type.EXACT && command.getLength() - 1 != paramLength) {
                                return new ErrorReply("ERR wrong number of arguments for '" + name + "' command");
                            }
                            if (aClass.getAnnotation(ParamLength.class).type() == ParamLength.Type.AT_LEAST && command.getLength() - 1 < paramLength) {
                                return new ErrorReply("ERR wrong number of arguments for '" + name + "' command");
                            }
                            if (aClass.getAnnotation(ParamLength.class).type() == ParamLength.Type.AT_MOST && command.getLength() - 1 > paramLength) {
                                return new ErrorReply("ERR wrong number of arguments for '" + name + "' command");
                            }
                        }
                        try {
                            Object[] objects = new Object[types.length];
                            command.toArguments(objects, types, ctx);
                            return (Reply<?>) method.invoke(context.getBean(aClass), objects);
                        } catch (Exception e) {
                            log.error("", e);
                            return new ErrorReply("ERR " + e.getMessage());
                        }
                    });
                }
            }
        }
    }

    private static final byte LOWER_DIFF = 'a' - 'A';

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) {
        val name = command.getName();
        // LowerCase bytes
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            if (b >= 'A' && b <= 'Z') {
                name[i] = (byte) (b + LOWER_DIFF);
            }
        }
        CommandWrapper wrapper = methods.get(new BytesKey(name));
        Reply<?> reply;
        if (wrapper == null) {
            reply = new ErrorReply("ERR unknown command `" + new String(name) + "`");
        } else {
            reply = wrapper.execute(ctx, command);
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
