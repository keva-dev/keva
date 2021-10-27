package dev.keva.server.command;

import com.google.inject.Inject;
import dev.keva.server.command.setup.CommandHandler;
import dev.keva.server.protocol.redis.BulkReply;
import dev.keva.store.StorageService;

import java.util.List;

public class Get implements CommandHandler {
    private final StorageService store;

    @Inject
    public Get(StorageService store) {
        this.store = store;
    }

    @Override
    public BulkReply handle(List<String> args) {
        String got = store.getString(args.get(1));
        return got != null ? new BulkReply(got) : BulkReply.NIL_REPLY;
    }
}
