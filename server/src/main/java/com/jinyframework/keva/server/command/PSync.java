package com.jinyframework.keva.server.command;

import com.jinyframework.keva.server.replication.master.ReplicationService;

import java.io.IOException;
import java.util.List;

public class PSync implements CommandHandler {

    private final ReplicationService replicationService;

    public PSync(ReplicationService replicationService) {
        this.replicationService = replicationService;
    }

    @Override
    public Object handle(List<String> args) {
        final String host = args.get(0);
        final String port = args.get(1);
        final String masterId = args.get(2);
        final String offset = args.get(3);

        try {
            return replicationService.performSync(host, port, masterId, Integer.parseInt(offset));
        } catch (IOException e) {
            return "null";
        }
    }
}
