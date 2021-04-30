package com.jinyframework.keva.server.command;

import com.jinyframework.keva.store.NoHeapStore;
import com.jinyframework.keva.server.storage.StorageFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Expire implements CommandHandler {
    private final Timer timer = new Timer();
    private final NoHeapStore kevaStore = StorageFactory.getNoHeapDBStore();

    @Override
    public Object handle(List<String> args) {
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    kevaStore.remove(args.get(0));
                }
            }, Long.parseLong(args.get(1)));
            return 1;
        } catch (Exception ignore) {
            return 0;
        }
    }
}
