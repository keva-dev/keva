package dev.keva.server.command.impl.connection;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import io.netty.channel.ChannelHandlerContext;

@Component
@CommandImpl("client")
@ParamLength(1)
public class Client {
    @Execute
    public Reply<?> execute(byte[] param, ChannelHandlerContext ctx) {
        String paramStr = new String(param);
        if (paramStr.equalsIgnoreCase("id")) {
            return new BulkReply(ctx.channel().id().asShortText());
        } else if (paramStr.equalsIgnoreCase("info")) {
            String info = String.format("id=%s, addr=%s\n", ctx.channel().id().asShortText(), ctx.channel().remoteAddress().toString());
            return new BulkReply(info);
        } else {
            return new ErrorReply("ERROR Unsupported query");
        }
    }
}
