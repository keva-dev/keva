package dev.keva.core.command.impl.generic;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.storage.KevaDatabase;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

@Component
@CommandImpl("type")
@ParamLength(1)
public class Type {
    private final KevaDatabase database;

    @Autowired
    public Type(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public StatusReply execute(byte[] key) {
        byte[] got = database.get(key);
        if (got == null) {
            return new StatusReply("none");
        }
        try {
            Object ignored = SerializationUtils.deserialize(got);
        } catch (SerializationException e) {
            return new StatusReply("string");
        }
        Object value = SerializationUtils.deserialize(got);
        try {
            String ignored = (String) value;
            return new StatusReply("string");
        } catch (ClassCastException e) {
            if (value instanceof HashMap) {
                return new StatusReply("hash");
            } else if (value instanceof LinkedList) {
                return new StatusReply("list");
            } else if (value instanceof HashSet) {
                return new StatusReply("set");
            } else {
                return new StatusReply("unknown");
            }
        }
    }
}
