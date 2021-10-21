package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.protocol.redis.IntegerReply;
import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Expire implements CommandHandler {
    private final Timer timer = new Timer();
    private final StorageService storageService;

    public Expire(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public IntegerReply handle(List<String> args) {
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    storageService.remove(args.get(0));
                }
            }, Long.parseLong(args.get(1)));
            return new IntegerReply(1);
        } catch (Exception ignore) {
            return new IntegerReply(0);
        }
    }
}
