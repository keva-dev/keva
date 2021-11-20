package dev.keva.server.command.impl.kql;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.*;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.impl.kql.manager.KqlManager;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;

import java.util.List;

@Component
@CommandImpl("kql")
@ParamLength(1)
public class Kql {
    private final KqlManager kqlManager;

    @Autowired
    public Kql(KqlManager kqlManager) {
        this.kqlManager = kqlManager;
    }

    @Execute
    public Reply<?> execute(byte[] sqlBytes) {
        String sql = new String(sqlBytes);
        Statement stmt = null;
        try {
            stmt = kqlManager.parse(sql);
        } catch (JSQLParserException e) {
            return new ErrorReply("ERR " + e.getMessage());
        }
        if (stmt instanceof CreateTable) {
            kqlManager.create(stmt);
            Reply<?>[] replies = new Reply[2];
            replies[0] = new StatusReply("DONE");
            replies[1] = new IntegerReply(0);
            return new MultiBulkReply(replies);
        } else if (stmt instanceof Insert) {
            kqlManager.insert(stmt);
            Reply<?>[] replies = new Reply[2];
            replies[0] = new StatusReply("DONE");
            replies[1] = new IntegerReply(1);
            return new MultiBulkReply(replies);
        } else if (stmt instanceof Select) {
            List<List<Object>> result = kqlManager.select(stmt);
            Reply<?>[] rowReplies = new Reply[result.size()];
            for (int i = 0; i < result.size(); i++) {
                Reply<?>[] columnReplies = new Reply[result.get(i).size()];
                for (int j = 0; j < result.get(i).size(); j++) {
                    if (result.get(i).get(j) instanceof String) {
                        columnReplies[j] = new BulkReply((String) result.get(i).get(j));
                    } else if (result.get(i).get(j) instanceof Integer) {
                        columnReplies[j] = new IntegerReply((Integer) result.get(i).get(j));
                    } else if (result.get(i).get(j) instanceof Boolean) {
                        columnReplies[j] = new IntegerReply((Boolean) result.get(i).get(j) ? 1 : 0);
                    }
                }
                rowReplies[i] = new MultiBulkReply(columnReplies);
            }
            return new MultiBulkReply(rowReplies);
        } else {
            return new ErrorReply("ERR unsupported statement");
        }
    }
}
