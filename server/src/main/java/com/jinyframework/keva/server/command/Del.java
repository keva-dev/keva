package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Del implements CommandHandler {
    private final StorageService storageService;

    public Del(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public String handle(List<String> args) {
        try {
            storageService.remove(args.get(0));
            return CommandConstant.SUCCESS_CODE;
        } catch (Exception ignore) {
            return CommandConstant.FAIL_CODE;
        }
    }
}
