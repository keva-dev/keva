package dev.keva.server.command;

import com.google.inject.Inject;
import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.IntegerReply;
import dev.keva.store.StorageService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Expire implements CommandHandler {
    private final Timer timer = new Timer();
    private final StorageService store;

    @Inject
    public Expire(StorageService store) {
        this.store = store;
    }

    @Override
    public IntegerReply handle(List<String> args) {
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    store.remove(args.get(1));
                }
            }, Long.parseLong(args.get(2)));
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }
}
