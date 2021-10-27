package dev.keva.server.command;

import com.google.inject.Inject;
import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.StatusReply;
import dev.keva.store.StorageService;

import java.util.List;

public class Set implements CommandHandler {
    private final StorageService store;

    @Inject
    public Set(StorageService store) {
        this.store = store;
    }

    @Override
    public StatusReply handle(List<String> args) {
        store.putString(args.get(1), args.get(2));
        return StatusReply.OK;
    }
}
