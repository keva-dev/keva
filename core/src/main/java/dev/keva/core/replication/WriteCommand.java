package dev.keva.core.replication;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

/**
 * For filtering out write commands and use with Jedis client
 */
public enum WriteCommand implements ProtocolCommand {
    SET, DEL;

    private final byte[] raw;

    WriteCommand() {
        raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }

}
