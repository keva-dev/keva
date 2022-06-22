package dev.keva.core.command.impl.zset;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.storage.KevaDatabase;
import dev.keva.util.DoubleUtil;
import dev.keva.util.hashbytes.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;

import static dev.keva.util.Constants.*;

@Component
@CommandImpl("zadd")
@ParamLength(type = ParamLength.Type.AT_LEAST, value = 3)
@Mutate
public final class ZAdd extends ZBase {
    private static final String XX = "xx";
    private static final String NX = "nx";
    private static final String GT = "gt";
    private static final String LT = "lt";
    private static final String INCR = "incr";
    private static final String CH = "ch";

    private final KevaDatabase database;

    @Autowired
    public ZAdd(KevaDatabase database) {
        super(database);
        this.database = database;
    }

    @Execute
    public Reply<?> execute(byte[][] params) {
        // Parse the flags, if any
        boolean xx = false, nx = false, gt = false, lt = false, incr = false;
        int argPos = 1, flags = 0;
        String arg;
        while (argPos < params.length) {
            arg = new String(params[argPos], StandardCharsets.UTF_8);
            if (XX.equalsIgnoreCase(arg)) {
                xx = true;
                flags |= FLAG_XX;
            } else if (NX.equalsIgnoreCase(arg)) {
                nx = true;
                flags |= FLAG_NX;
            } else if (GT.equalsIgnoreCase(arg)) {
                gt = true;
                flags |= FLAG_GT;
            } else if (LT.equalsIgnoreCase(arg)) {
                lt = true;
                flags |= FLAG_LT;
            } else if (INCR.equalsIgnoreCase(arg)) {
                incr = true;
                flags |= FLAG_INCR;
            } else if (CH.equalsIgnoreCase(arg)) {
                flags |= FLAG_CH;
            } else {
                break;
            }
            ++argPos;
        }

        int numMembers = params.length - argPos;
        if (numMembers % 2 != 0) {
            return ErrorReply.SYNTAX_ERROR;
        }
        numMembers /= 2;

        if (nx && xx) {
            return ErrorReply.ZADD_NX_XX_ERROR;
        }
        if ((gt && nx) || (lt && nx) || (gt && lt)) {
            return ErrorReply.ZADD_GT_LT_NX_ERROR;
        }
        if (incr && numMembers > 1) {
            return ErrorReply.ZADD_INCR_ERROR;
        }

        // Parse the key and value
        final SimpleEntry<Double, BytesKey>[] members = new SimpleEntry[numMembers];
        double score;
        String rawScore;
        for (int memberIndex = 0; memberIndex < numMembers; ++memberIndex) {
            try {
                rawScore = new String(params[argPos++], StandardCharsets.UTF_8);
                if (rawScore.equalsIgnoreCase("inf") || rawScore.equalsIgnoreCase("infinity")
                        || rawScore.equalsIgnoreCase("+inf") || rawScore.equalsIgnoreCase("+infinity")
                ) {
                    score = Double.POSITIVE_INFINITY;
                } else if (rawScore.equalsIgnoreCase("-inf") || rawScore.equalsIgnoreCase("-infinity")) {
                    score = Double.NEGATIVE_INFINITY;
                } else {
                    score = Double.parseDouble(rawScore);
                }
            } catch (final NumberFormatException ignored) {
                // return on first bad input
                return ErrorReply.ZADD_SCORE_FLOAT_ERROR;
            }
            members[memberIndex] = new SimpleEntry<>(score, new BytesKey(params[argPos++]));
        }

        if (incr) {
            Double result = this.increaseBy(params[0], members[0].getKey(), members[0].getValue(), flags);
            return result == null ? BulkReply.NIL_REPLY : new BulkReply(DoubleUtil.toString(result));
        }
        int result = this.add(params[0], members, flags);
        return new IntegerReply(result);
    }
}
