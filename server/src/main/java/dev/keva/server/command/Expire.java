package dev.keva.server.command;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.store.KevaDatabase;

import java.util.Timer;
import java.util.TimerTask;

@Component
@CommandImpl("expire")
@ParamLength(2)
public class Expire {
    @Autowired
    private KevaDatabase database;

    private final Timer timer = new Timer();

    @Execute
    public IntegerReply execute(byte[] key, byte[] time) {
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    database.remove(key);
                }
            }, Long.parseLong(new String(time)) * 1000);
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }

}
