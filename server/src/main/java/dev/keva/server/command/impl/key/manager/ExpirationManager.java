package dev.keva.server.command.impl.key.manager;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.store.KevaDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ExpirationManager {
    private static final byte[] EXP_POSTFIX = new byte[]{(byte) 0x7f, (byte) 0x2f, (byte) 0x61, (byte) 0x74};
    private final KevaDatabase database;
    private final ExecutorService expireExecutor = Executors.newFixedThreadPool(1);

    @Autowired
    public ExpirationManager(KevaDatabase database) {
        this.database = database;
    }

    public void expireAt(byte[] key, long at) {
        byte[] expireKey = getExpireKey(key);
        byte[] timestampInMillis = Longs.toByteArray(at);
        if (at <= System.currentTimeMillis()) {
            executeExpire(key);
        } else {
            database.put(expireKey, timestampInMillis);
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
            database.remove(key);
            clearExpiration(key);
        });
    }
}
