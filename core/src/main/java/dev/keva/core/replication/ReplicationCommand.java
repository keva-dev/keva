package dev.keva.core.replication;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public enum ReplicationCommand implements ProtocolCommand {
    PSYNC;

    private final byte[] raw;

    ReplicationCommand() {
        raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
        return raw;

    }

}
