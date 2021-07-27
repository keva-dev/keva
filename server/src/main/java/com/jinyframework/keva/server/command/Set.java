package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.ServiceInstance;
import com.jinyframework.keva.server.storage.StorageService;

import java.util.List;

public class Set implements CommandHandler {
    private final StorageService storageService = ServiceInstance.getStorageService();

    @Override
    public String handle(List<String> args) {
        try {
            final boolean success = storageService.putString(args.get(0), args.get(1));
            if (success) {
                return CommandConstant.SUCCESS_CODE;
            }
            return CommandConstant.FAIL_CODE;
        } catch (Exception ignore) {
            return CommandConstant.FAIL_CODE;
        }
    }
}
