package dev.keva.core.command.mapping;

import dev.keva.core.aof.AOFContainer;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.connection.manager.AuthManager;
import dev.keva.core.command.impl.transaction.manager.TransactionManager;
import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.util.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.store.KevaDatabase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.SerializationException;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class CommandMapper {

    private static final Set<String> EXCLUSIVE_COMMANDS = new HashSet<>(Arrays.asList(
            "exec", "expire", "expireat", "restore", "flushdb"));

    @Getter
    private final Map<BytesKey, CommandWrapper> methods = new HashMap<>();

    @Autowired
    private KevaIoC context;

    @Autowired
    private TransactionManager txManager;

    @Autowired
    private AuthManager authManager;

    @Autowired
    private KevaDatabase database;

    @Autowired
    private KevaConfig kevaConfig;

    @Autowired
    private AOFContainer aof;

    public void init() {
        val reflections = new Reflections("dev.keva.core.command.impl");
        val annotated = reflections.getTypesAnnotatedWith(CommandImpl.class);
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
                    val password = kevaConfig.getPassword();
                    val isAuthEnabled = password != null && password.length() > 0;

                    methods.put(new BytesKey(name.getBytes()), (ctx, command) -> {
                        if (isAuthEnabled && !Arrays.equals(command.getName(), "auth".getBytes())) {
                            boolean authenticated = authManager.isAuthenticated(ctx.channel());
                            if (!authenticated) {
                                return new ErrorReply("ERR NOAUTH Authentication required.");
                            }
                        }

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
                            val lock = database.getLock();
                            boolean locked = false, exclusive = false, writeToAOF = isAoF && isMutate;
                            try {
                                Object[] objects = new Object[types.length];
                                command.toArguments(objects, types, ctx);
                                if (ctx != null) {
                                    locked = true;
                                    if (isMutate || EXCLUSIVE_COMMANDS.contains(name)) {
                                        lock.exclusiveLock();
                                        exclusive = true;
                                        if (writeToAOF) {
                                            try {
                                                aof.write(command);
                                            } catch (Exception e) {
                                                log.error("Error writing to AOF", e);
                                            }
                                        }
                                    } else {
                                        lock.sharedLock();
                                    }
                                }
                                // If not in AOF mode, then recycle(),
                                // else, the command will be recycled in the AOF dump
                                if (!kevaConfig.getAof()) {
                                    command.recycle();
                                }
                                return (Reply<?>) method.invoke(instance, objects);
                            } finally {
                                if (locked) {
                                    if (exclusive) {
                                        lock.exclusiveUnlock();
                                    } else {
                                        lock.sharedUnlock();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            if (e instanceof InvocationTargetException) {
                                if (e.getCause() instanceof SerializationException || e.getCause() instanceof ClassCastException) {
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
