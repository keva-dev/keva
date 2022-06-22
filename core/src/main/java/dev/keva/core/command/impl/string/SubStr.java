package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("substr")
@ParamLength(3)
public class SubStr {
    private final KevaDatabase database;

    @Autowired
    public SubStr(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] start, byte[] end) {
        int startInt = Integer.parseInt(new String(start, StandardCharsets.UTF_8));
        int endInt = Integer.parseInt(new String(end, StandardCharsets.UTF_8));
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        String valueStr = new String(value, StandardCharsets.UTF_8);

        // Convert negative indexes to positive ones
        if (startInt < 0 && endInt < 0 && startInt > endInt) {
            return null;
        }
        if (startInt < 0) startInt = valueStr.length() + startInt;
        if (endInt < 0) endInt = valueStr.length() + endInt;
        if (startInt < 0) startInt = 0;
        if (endInt < 0) endInt = 0;
        if (endInt >= valueStr.length()) endInt = valueStr.length() - 1;

        byte[] result;
        if (startInt > endInt) {
            result = "".getBytes();
        } else {
            result = valueStr.substring(startInt, endInt + 1).getBytes(StandardCharsets.UTF_8);
        }

        return new BulkReply(result);
    }
}
