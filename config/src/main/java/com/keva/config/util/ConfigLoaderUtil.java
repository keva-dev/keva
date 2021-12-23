package com.keva.config.util;

import com.keva.config.annotation.CliProp;
import com.keva.config.annotation.CliPropType;
import com.keva.config.annotation.ConfigProp;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Properties;

public class ConfigLoaderUtil {
    @SneakyThrows
    public static <T> T fromProperties(@NonNull Properties props, Class<T> clazz) {
        T configHolder = clazz.getDeclaredConstructor().newInstance();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ConfigProp.class)) {
                field.setAccessible(true);
                ConfigProp annotation = field.getAnnotation(ConfigProp.class);
                Object value = parse(props.getProperty(annotation.name(), annotation.defaultVal()), field.getType());
                field.set(configHolder, value);
            }
        }

        return configHolder;
    }

    @SneakyThrows
    public static <T> T fromArgs(@NonNull ArgsHolder args, Class<T> clazz) {
        T configHolder = clazz.getDeclaredConstructor().newInstance();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(CliProp.class)) {
                field.setAccessible(true);
                CliProp cliAnnotate = field.getAnnotation(CliProp.class);
                String strVal = null;
                if (cliAnnotate.type() == CliPropType.VAL) {
                    strVal = args.getArgVal(cliAnnotate.name());
                } else if (cliAnnotate.type() == CliPropType.FLAG) {
                    strVal = args.getFlag(cliAnnotate.name());
                }
                if (strVal != null) {
                    Object value = parse(strVal, field.getType());
                    field.set(configHolder, value);
                }
            }
        }

        return configHolder;
    }

    @SneakyThrows
    public static <T> void merge(T obj1, T obj2) {
        if (obj1 != null && obj2 != null && !obj1.equals(obj2)) {
            for (Field field : obj2.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object overrideVal = field.get(obj2);
                if (overrideVal != null) {
                    field.set(obj1, overrideVal);
                }
            }
        }
    }

    @SneakyThrows
    private static <T> T parse(String s, Class<T> clazz) {
        return clazz.getConstructor(String.class).newInstance(s);
    }
}
