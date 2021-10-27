package dev.keva.server.command;

import com.google.inject.Inject;
import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.IntegerReply;
import dev.keva.store.StorageService;

import java.util.List;

public class Del implements CommandHandler {
    private final StorageService store;

    @Inject
    public Del(StorageService store) {
        this.store = store;
    }

    @Override
    public IntegerReply handle(List<String> args) {
        return store.remove(args.get(1))
                ? new IntegerReply(1)
                : new IntegerReply(0);
    }
}
