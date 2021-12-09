package dev.keva.server.command.mapping;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.Mutate;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.aof.AOFOperations;
import dev.keva.server.command.impl.transaction.manager.TransactionManager;
import dev.keva.server.config.KevaConfig;
import dev.keva.store.KevaDatabase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class CommandMapper {
    @Getter
    private final Map<BytesKey, CommandWrapper> methods = new HashMap<>();

    @Autowired
    private KevaIoC context;

    @Autowired
    private TransactionManager txManager;

    @Autowired
    private KevaDatabase database;

    @Autowired
    private KevaConfig kevaConfig;

    @Autowired
    private AOFOperations aof;

    public void init() {
        Reflections reflections = new Reflections("dev.keva.server.command.impl");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(CommandImpl.class);
        val isAoF = kevaConfig.getAof();
        for (Class<?> aClass : annotated) {
            for (val method : aClass.getMethods()) {
                if (method.isAnnotationPresent(Execute.class)) {
                    val types = method.getParameterTypes();
                    val name = aClass.getAnnotation(CommandImpl.class).value();
                    val paramLength = aClass.getAnnotation(ParamLength.class) != null ? aClass.getAnnotation(ParamLength.class).value() : -1;
                    val paramLengthType = aClass.getAnnotation(ParamLength.class) != null ? aClass.getAnnotation(ParamLength.class).type() : null;
                    val instance = context.getBean(aClass);
                    val isMutate = aClass.getAnnotation(Mutate.class) != null;

                    methods.put(new BytesKey(name.getBytes()), (ctx, command) -> {
                        if (ctx != null) {
                            val txContext = txManager.getTransactions().get(ctx.channel());
                            if (txContext != null && txContext.isQueuing()) {
                                if (!Arrays.equals(command.getName(), "exec".getBytes()) && !Arrays.equals(command.getName(), "discard".getBytes())) {
                                    ErrorReply errorReply = CommandValidate.validate(paramLengthType, paramLength, command.getLength(), name);
                                    if (errorReply == null) {
                                        txContext.getCommandDeque().add(command);
                                        return new StatusReply("QUEUED");
                                    } else {
                                        txContext.discard();
                                        return errorReply;
                                    }
                                }
                            }
                        }

                        ErrorReply errorReply = CommandValidate.validate(paramLengthType, paramLength, command.getLength(), name);
                        if (errorReply != null) {
                            return errorReply;
                        }
                        try {
                            if (ctx != null && isAoF && isMutate) {
                                val lock = database.getLock();
                                lock.lock();
                                try {
                                    aof.write(command);
                                } catch (Exception e) {
                                    log.error("Error writing to AOF", e);
                                } finally {
                                    lock.unlock();
                                }
                            }
                            Object[] objects = new Object[types.length];
                            command.toArguments(objects, types, ctx);
                            return (Reply<?>) method.invoke(instance, objects);
                        } catch (Exception e) {
                            log.error("", e);
                            if (e instanceof InvocationTargetException) {
                                if (e.getCause() instanceof ClassCastException) {
                                    return new ErrorReply("ERR WRONGTYPE Operation against a key holding the wrong kind of value");
                                }
                                return new ErrorReply("ERR " + e.getCause().getMessage());
                            }
                            return new ErrorReply("ERR " + e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
