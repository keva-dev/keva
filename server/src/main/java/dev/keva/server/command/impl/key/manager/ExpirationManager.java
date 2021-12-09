package dev.keva.server.command.impl.key.manager;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.server.command.aof.AOFOperations;
import dev.keva.server.config.KevaConfig;
import dev.keva.store.KevaDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

@Component
public class ExpirationManager {
    /**
     * the 4 bytes is equivalent to '[DELETE]/at' in UTF-8 encoding, with DELETE being a special character
     * so user is won't to accidentally set a key with the postfix included in normal usage,
     * but still has the option to
     */
    private static final byte[] EXP_POSTFIX = new byte[]{(byte) 0x7f, (byte) 0x2f, (byte) 0x61, (byte) 0x74};
    private final KevaDatabase database;
    private final ExecutorService expireExecutor = Executors.newFixedThreadPool(1);
    private final KevaConfig kevaConfig;
    private final AOFOperations aof;

    @Autowired
    public ExpirationManager(KevaDatabase database, KevaConfig kevaConfig, AOFOperations aof) {
        this.database = database;
        this.kevaConfig = kevaConfig;
        this.aof = aof;
    }

    public void expireAt(byte[] key, long timestampInMillis) {
        byte[] expireKey = getExpireKey(key);
        byte[] timestampBytes = Longs.toByteArray(timestampInMillis);
        if (timestampInMillis <= System.currentTimeMillis()) {
            executeExpire(key);
        } else {
            database.put(expireKey, timestampBytes);
        }
    }

    public void expireAfter(byte[] key, long afterInSeconds) {
        expireAt(key, System.currentTimeMillis() + afterInSeconds * 1000);
    }

    private byte[] getExpireKey(byte[] key) {
        return Bytes.concat(key, EXP_POSTFIX);
    }

    public boolean isExpirable(byte[] key) {
        byte[] longInBytes = database.get(getExpireKey(key));
        if (longInBytes == null) {
            return false;
        } else {
            return Longs.fromByteArray(longInBytes) <= System.currentTimeMillis();
        }
    }

    public void clearExpiration(byte[] key) {
        database.remove(getExpireKey(key));
    }

    public void executeExpire(byte[] key) {
        expireExecutor.submit(() -> {
            if (kevaConfig.getAof()) {
                byte[][] data = new byte[2][];
                data[0] = "delete".getBytes();
                data[1] = key;
                Command command = new Command(data, false);
                Lock lock = database.getLock();
                lock.lock();
                try {
                    aof.write(command);
                    database.remove(key);
                    clearExpiration(key);
                } finally {
                    lock.unlock();
                }
            } else {
                database.remove(key);
                clearExpiration(key);
            }
        });
    }

    public void move(byte[] key, byte[] newName) {
        byte[] timestampBytes = database.get(getExpireKey(key));
        if (timestampBytes != null) {
            database.put(getExpireKey(newName), timestampBytes);
            clearExpiration(key);
        }
    }
}
