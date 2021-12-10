package dev.keva.server.command.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ParamLength {
    int value() default -1;

    Type type() default Type.EXACT;

    enum Type {
        EXACT, AT_LEAST, AT_MOST
    }
}
