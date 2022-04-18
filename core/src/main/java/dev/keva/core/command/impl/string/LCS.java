package dev.keva.core.command.impl.string;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.transaction.Multi;
import dev.keva.core.exception.CommandException;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.store.KevaDatabase;

import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.*;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("stralgo")
@ParamLength(type = AT_LEAST, value = 2)
public class LCS {
    private final String LEN = "len";
    private final String IDX = "idx";
    private final String MINMATCHLEN = "minmatchlen";
    private final String WITHMATCHLEN = "withmatchlen";
    private final String STRINGS = "strings";
    private final String KEYS = "keys";

    private final KevaDatabase database;

    @Autowired
    public LCS(KevaDatabase database) {
        this.database = database;
    }

    @Execute
    public MultiBulkReply execute(byte[][] params) {
        List<Reply> repliesList = new ArrayList<Reply>();
        // TODO refactor not to use this anymore

        long minmatchlen = 0;
        boolean getlen = false, getidx = false, withmatchlen = false;
        String a = null, b = null;
        int flags = 0;

        // TODO a lot of ifelse for param handling, ggwp
        for(int i = 1; i < params.length; i++) {
            String opt = new String(params[i], StandardCharsets.UTF_8);
            int moreArgs = params.length - i;

            if(IDX.compareToIgnoreCase(opt) == 0) {
                getidx = true;
            } else if (LEN.compareToIgnoreCase(opt) == 0) {
                getlen = true;
            } else if (WITHMATCHLEN.compareToIgnoreCase(opt) == 0) {
                withmatchlen = true;
            } else if (MINMATCHLEN.compareToIgnoreCase(opt) == 0  && moreArgs > 0) {
                minmatchlen = Long.parseLong(new String(params[i+1]));
                // TODO Catch parsing failure with appropriate response here
                if (minmatchlen < 0) minmatchlen = 0;
                i++;
            } else if (STRINGS.compareToIgnoreCase(opt) == 0 && moreArgs > 1) {
                if(a != null) {
                    throw new CommandException("Either use STRINGS or KEYS");
                }
                a = new String(params[i+1], StandardCharsets.UTF_8);
                b = new String(params[i+2], StandardCharsets.UTF_8);
                i += 2;
            } else if (KEYS.compareToIgnoreCase(opt) == 0 && moreArgs > 1) {
                if(a != null) {
                    throw new CommandException("Either use STRINGS or KEYS");
                }
                a = new String(database.get(params[i+1]), StandardCharsets.UTF_8);
                b = new String(database.get(params[i+2]), StandardCharsets.UTF_8);
                i += 2;
            } else {
                throw new CommandException("syntax error");
            }
        }

        if (a == null) {
            throw new CommandException("Please specify two strings: STRINGS or KEYS options are mandatory");
        } else if (getlen && getidx) {
            throw new CommandException("If you want bot the length and indexes, please just use IDX");
        }

        if (a.length() > Integer.MAX_VALUE || b.length() > Integer.MAX_VALUE) {
            throw new CommandException("String too long for LCS");
        }

        final int alen = a.length();
        final int blen = b.length();
        final int DP_TBL_SIZE1 = alen + 1;
        final int DP_TBL_SIZE2 = blen + 1;
        int dpTbl[][] = new int[DP_TBL_SIZE1][DP_TBL_SIZE2];
        // TODO this can be optimized further by using 1 dimensional array
        // dpTbl[i][j] -> dpTbl[j + (i*(blen+1))

        for (int i = 1; i <= alen; i++) {
            for (int j = 1; j <= blen; j++) {
                if (a.charAt(i-1) != b.charAt(j-1)) {
                    dpTbl[i][j] = Math.max(dpTbl[i][j-1], dpTbl[i-1][j]);
                } else {
                    dpTbl[i][j] = dpTbl[i-1][j-1] + 1;
                }
            }
        }
        int idx = dpTbl[alen][blen];
        String result = null;

        boolean computelcs = getidx || !getlen;

        // Start traceback lcs tbl
        int i = alen, j = blen;
        List<List<List>> idxList = new ArrayList(); // omg what is this
        StringBuilder res = new StringBuilder();
        int pos1 = i, pos2 = j;
        boolean matched = false;
        while (computelcs && i > 0 && j > 0) {
            //if(dpTbl[i-1][j] == dpTbl[i][j - 1]) {
            if (a.charAt(i-1) == b.charAt(j-1)) {
                if (!matched) {
                    pos1 = i; pos2 = j;
                    matched = true;
                }
                res.append(a.charAt(i - 1));
                i--;
                j--;
            } else {
                if (matched) {
                    matched = false;
                    idxList.add(Arrays.asList(
                        Arrays.asList(i, pos1 - 1),
                        Arrays.asList(j, pos2 - 1),
                        Arrays.asList(pos1 - i)
                    ));
                }
                if(dpTbl[i-1][j] > dpTbl[i][j-1]) {
                    i--;
                } else {
                    j--;
                }
            }
        }
        if (matched == true) {
            idxList.add(Arrays.asList(
                    Arrays.asList(i, pos1 - 1),
                    Arrays.asList(j, pos2 - 1),
                    Arrays.asList(pos1 - i)
            ));
        }
        // end traceback

        if (getidx) {
            repliesList.add(new BulkReply("matches"));
            List<Reply> matchesReply = new ArrayList<Reply>();

            for (List<List> el : idxList) {
                int matchedLen = (int) el.get(2).get(0);
                if (minmatchlen == 0 || matchedLen >= minmatchlen) {
                    List<Reply> matchReplyList = new ArrayList<Reply>();
                    matchReplyList.add(new MultiBulkReply(
                            new Reply[] {new IntegerReply((int) el.get(0).get(0)), new IntegerReply((int) el.get(0).get(1))}
                        )
                    );
                    matchReplyList.add(new MultiBulkReply(
                            new Reply[] {new IntegerReply((int) el.get(1).get(0)), new IntegerReply((int) el.get(1).get(1))}
                        )
                    );
                    if(withmatchlen) {
                        matchReplyList.add(new IntegerReply(matchedLen));
                    }

                    Reply[] matchReply = new Reply[matchReplyList.size()];
                    matchReplyList.toArray(matchReply);
                    matchesReply.add(new MultiBulkReply(matchReply));
                }
            }
            Reply[] replies = new Reply[matchesReply.size()];
            matchesReply.toArray(replies);

            repliesList.add(new MultiBulkReply(replies));
            repliesList.add(new BulkReply("len"));
            repliesList.add(new IntegerReply(dpTbl[alen][blen]));
        } else if (getlen) {
            repliesList.add(new IntegerReply(dpTbl[alen][blen]));
        } else {
            repliesList.add(new BulkReply(res.toString()));
        }

        Reply[] replies = new Reply[repliesList.size()];
        repliesList.toArray(replies);

        return new MultiBulkReply(replies);
    }
}
