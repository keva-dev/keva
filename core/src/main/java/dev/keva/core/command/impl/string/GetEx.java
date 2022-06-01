package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.storage.KevaDatabase;

import java.nio.charset.StandardCharsets;

@Component
@CommandImpl("getex")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 2)
public class GetEx {
    private final KevaDatabase database;

    private final String EX = "EX";
    private final String PX = "PX";
    private final String EXAT = "EXAT";
    private final String PXAT = "PXAT";
    private final String PERSIST = "PERSIST";

    private final int EX_FLG = 1 << 2;
    private final int PX_FLG = 1 << 3;
    private final int EXAT_FLG = 1 << 6;
    private final int PXAT_FLG = 1 << 7;
    private final int PERSIST_FLG = 1 << 8;

    @Autowired
    public GetEx(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public BulkReply execute(byte[][] params) {
        int flgs = 0;
        String nextArg = null;
        long expireTime = 0;

        for (int i = 1; i < params.length; i++) {
            String opt = new String(params[i], StandardCharsets.UTF_8);
            nextArg = i == params.length - 1 ? null : new String(params[i + 1], StandardCharsets.UTF_8);
            if (EX.equalsIgnoreCase(opt) && nextArg != null && !isHavingOneOfTheseFlgs(flgs, EX_FLG, PX_FLG, EXAT_FLG, PXAT_FLG, PERSIST_FLG)) {
                flgs |= EX_FLG;
                i++;
            } else if (PX.equalsIgnoreCase(opt) && nextArg != null && !isHavingOneOfTheseFlgs(flgs, EX_FLG, PX_FLG, EXAT_FLG, PXAT_FLG, PERSIST_FLG)) {
                flgs |= PX_FLG;
                i++;
            } else if (EXAT.equalsIgnoreCase(opt) && nextArg != null && !isHavingOneOfTheseFlgs(flgs, EX_FLG, PX_FLG, EXAT_FLG, PXAT_FLG, PERSIST_FLG)) {
                flgs |= EXAT_FLG;
                i++;
            } else if (PXAT.equalsIgnoreCase(opt) && nextArg != null && !isHavingOneOfTheseFlgs(flgs, EX_FLG, PX_FLG, EXAT_FLG, PXAT_FLG, PERSIST_FLG)) {
                flgs |= PXAT_FLG;
                i++;
            } else if (PERSIST.equalsIgnoreCase(opt) && nextArg == null && !isHavingOneOfTheseFlgs(flgs, EX_FLG, PX_FLG, EXAT_FLG, PXAT_FLG, PERSIST_FLG)) {
                flgs |= PERSIST_FLG;
            } else {
                throw new CommandException("Syntax error");
            }
        }
        byte[] got = database.get(params[0]);
        if (got == null) {
            return BulkReply.NIL_REPLY;
        }

        if (isHavingOneOfTheseFlgs(flgs, PERSIST_FLG)) {
            database.removeExpire(params[0]);
        } else {
            try {
                expireTime = Long.parseLong(nextArg);
            } catch (NumberFormatException e) {
                throw new CommandException("value is not an integer or out of range");
            }
            if (expireTime < 1) {
                throw new CommandException("invalid expire time in 'getex' command");
            }

            long expireAt = expireTime;
            if (isHavingOneOfTheseFlgs(flgs, EX_FLG, EXAT_FLG)) {
                expireAt *= 1000;
            }
            if (isHavingOneOfTheseFlgs(flgs, EX_FLG, PX_FLG)) {
                expireAt += System.currentTimeMillis();
            }

            if (expireAt < System.currentTimeMillis()) {
                database.remove(params[0]);
            } else {
                database.setExpiration(params[0], expireAt);
            }
        }
        return new BulkReply(got);
    }

    private boolean isHavingOneOfTheseFlgs(int flgs, int... flgsToCompare) {
        for (int j : flgsToCompare) {
            if ((flgs & j) != 0) {
                return true;
            }
        }
        return false;
    }

}
