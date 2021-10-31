package dev.keva.server.command.mapping;

import dev.keva.ioc.KevaIoC;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.hashbytes.BytesKey;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class CommandMapper {
    private final Map<BytesKey, CommandWrapper> methods = new HashMap<>();

    @Autowired
    private KevaIoC context;

    public Map<BytesKey, CommandWrapper> getMethods() {
        return methods;
    }

    public void init() {
        Reflections reflections = new Reflections("dev.keva.server.command.impl");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(CommandImpl.class);
        for (Class<?> aClass : annotated) {
            for (val method : aClass.getMethods()) {
                if (method.isAnnotationPresent(Execute.class)) {
                    val types = method.getParameterTypes();
                    val name = aClass.getAnnotation(CommandImpl.class).value();
                    val paramLength = aClass.getAnnotation(ParamLength.class) != null ? aClass.getAnnotation(ParamLength.class).value() : -1;
                    val paramLengthType = aClass.getAnnotation(ParamLength.class) != null ? aClass.getAnnotation(ParamLength.class).type() : null;
                    val instance = context.getBean(aClass);
                    methods.put(new BytesKey(name.getBytes()), (ctx, command) -> {
                        if (paramLength != -1 && paramLengthType != null) {
                            val commandLength = command.getLength();
                            if (paramLengthType == ParamLength.Type.EXACT && commandLength - 1 != paramLength) {
                                return new ErrorReply("ERR wrong number of arguments for '" + name + "' command");
                            }
                            if (paramLengthType == ParamLength.Type.AT_LEAST && commandLength - 1 < paramLength) {
                                return new ErrorReply("ERR wrong number of arguments for '" + name + "' command");
                            }
                            if (paramLengthType == ParamLength.Type.AT_MOST && commandLength - 1 > paramLength) {
                                return new ErrorReply("ERR wrong number of arguments for '" + name + "' command");
                            }
                        }
                        try {
                            Object[] objects = new Object[types.length];
                            command.toArguments(objects, types, ctx);
                            return (Reply<?>) method.invoke(instance, objects);
                        } catch (Exception e) {
                            log.error("", e);
                            return new ErrorReply("ERR " + e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
