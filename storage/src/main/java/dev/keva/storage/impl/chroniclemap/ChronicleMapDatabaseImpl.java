package dev.keva.storage.impl.chroniclemap;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.keva.storage.KevaDatabase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.values.Values;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static dev.keva.storage.constant.DatabaseConstants.EXPIRE_POSTFIX;

@Slf4j
public class ChronicleMapDatabaseImpl implements KevaDatabase {
    @Getter
    private final Lock lock = new ReentrantLock();
    private ChronicleMap<byte[], byte[]> chronicleMap;

    public ChronicleMapDatabaseImpl(ChronicleMapConfig config) {
        try {
            ChronicleMapBuilder<byte[], byte[]> mapBuilder = ChronicleMapBuilder.of(byte[].class, byte[].class)
                    .name("keva-chronicle-map")
                    .averageKey("SampleSampleSampleKey".getBytes())
                    .averageValue("SampleSampleSampleSampleSampleSampleValue".getBytes())
                    .entries(1_000_000);

            boolean shouldPersist = config.getIsPersistence();
            if (shouldPersist) {
                String workingDir = config.getWorkingDirectory();
                String location = workingDir.equals("./") ? "" : workingDir + "/";
                File file = new File(location + "dump.kdb");
                this.chronicleMap = mapBuilder.createPersistedTo(file);
            } else {
                this.chronicleMap = mapBuilder.create();
            }
        } catch (IOException e) {
            log.error("Failed to create ChronicleMap: ", e);
        }
    }

    @Override
    public void flush() {
        chronicleMap.clear();
    }

    @Override
    public byte[] get(byte[] key) {
        if (isExpired(key)) {
            chronicleMap.remove(key);
        }
        return chronicleMap.get(key);
    }

    @Override
    public void put(byte[] key, byte[] val) {
        chronicleMap.put(key, val);
        chronicleMap.remove(getExpireKey(key));
    }

    @Override
    public boolean remove(byte[] key) {
        chronicleMap.remove(getExpireKey(key));
        return chronicleMap.remove(key) != null;
    }

    @Override
    public boolean rename(byte[] key, byte[] newKey) {
        byte[] moveValue = chronicleMap.get(key);
        if (moveValue == null) {
            return false;
        }
        chronicleMap.put(newKey, moveValue);
        chronicleMap.remove(key);
        byte[] oldExpireKey = getExpireKey(key);
        byte[] timestampBytes = chronicleMap.get(oldExpireKey);
        if (timestampBytes != null) {
            chronicleMap.put(getExpireKey(newKey), timestampBytes);
            chronicleMap.remove(oldExpireKey);
        }
        return true;
    }

    @Override
    public Set<byte[]> keySet() {
        return chronicleMap.keySet();
    }

    @Override
    public void setExpiration(byte[] key, long timestampInMillis) {
        byte[] expireKey = getExpireKey(key);
        byte[] timestampBytes = Longs.toByteArray(timestampInMillis);
        if (timestampInMillis <= System.currentTimeMillis()) {
            chronicleMap.remove(expireKey);
        } else {
            chronicleMap.put(expireKey, timestampBytes);
        }
    }

    @Override
    public void removeExpire(byte[] key) {
        byte[] expireKey = getExpireKey(key);
        chronicleMap.remove(expireKey);
    }

    private byte[] getExpireKey(byte[] key) {
        return Bytes.concat(key, EXPIRE_POSTFIX);
    }

    private boolean isExpired(byte[] key) {
        byte[] longInBytes = chronicleMap.get(getExpireKey(key));
        if (longInBytes == null) {
            return false;
        } else {
            return Longs.fromByteArray(longInBytes) <= System.currentTimeMillis();
        }
    }
}
