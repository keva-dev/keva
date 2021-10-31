package dev.keva.server.core;

import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.wrapper.CommandWrapper;
import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Sharable
public class NettyChannelHandler extends SimpleChannelInboundHandler<Command> {
    private final ConcurrentMap<BytesKey, CommandWrapper> methods = new ConcurrentHashMap<>();

    public NettyChannelHandler() {
        Reflections reflections = new Reflections("dev.keva.server.command");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(CommandImpl.class);
        for (Class<?> aClass : annotated) {
            Object classInstance;
            try {
                classInstance = aClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Error while instantiating class " + aClass.getName(), e);
                return;
            }
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
                            return (Reply<?>) method.invoke(classInstance, objects);
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
