package dev.keva.core.command.impl.connection;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.connection.manager.AuthManager;
import dev.keva.core.config.KevaConfig;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.ErrorReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import io.netty.channel.ChannelHandlerContext;

@Component
@CommandImpl("auth")
@ParamLength(1)
public class Auth {
    private final KevaConfig kevaConfig;
    private final AuthManager authManager;

    @Autowired
    public Auth(KevaConfig kevaConfig, AuthManager authManager) {
        this.kevaConfig = kevaConfig;
        this.authManager = authManager;
    }

    @Execute
    public Reply<?> execute(byte[] password, ChannelHandlerContext ctx) {
        String passwordString = new String(password);
        if (kevaConfig.getPassword().equals(passwordString)) {
            authManager.authenticate(ctx.channel());
            return new StatusReply("OK");
        }
        return new ErrorReply("ERR WRONGPASS invalid password.");
    }
}
