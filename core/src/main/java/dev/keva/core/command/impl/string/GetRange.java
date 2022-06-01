package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("getrange")
@ParamLength(type = EXACT, value = 3)
public class GetRange {
    private final KevaDatabase database;

    @Autowired
    public GetRange(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[] key, byte[] start, byte[] end) {
        byte[] value = database.get(key);
        int startInt = Integer.parseInt(new String(start, StandardCharsets.UTF_8));
        int endInt = Integer.parseInt(new String(end, StandardCharsets.UTF_8));

        // convert negative indexes to positive ones
        if (startInt < 0 && endInt < 0 && startInt > endInt) {
            return null;
        }
        if (startInt < 0) startInt = value.length + startInt;
        if (endInt < 0) endInt = value.length + endInt;
        if (startInt < 0) startInt = 0;
        if (endInt < 0) endInt = 0;
        if (endInt >= value.length) endInt = value.length - 1;

        byte[] result;
        if (startInt > endInt) {
            result = "".getBytes();
        } else {
            result = Arrays.copyOfRange(value, startInt, endInt + 1);
        }
        return new BulkReply(result);
    }
}
