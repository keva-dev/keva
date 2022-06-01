package dev.keva.core.command.impl.zset;

import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesKey;
import lombok.NonNull;
import org.apache.commons.lang3.SerializationUtils;

import java.util.AbstractMap;

import static dev.keva.util.Constants.FLAG_CH;
import static dev.keva.util.Constants.FLAG_GT;
import static dev.keva.util.Constants.FLAG_LT;
import static dev.keva.util.Constants.FLAG_NX;
import static dev.keva.util.Constants.FLAG_XX;

public abstract class ZBase {
    private final KevaDatabase database;

    public ZBase(KevaDatabase database) {
        this.database = database;
    }

    protected Double increaseBy(byte[] key, Double incr, BytesKey e, int flags) {
        byte[] value = database.get(key);
        ZSet zSet;
        zSet = value == null ? new ZSet() : (ZSet) SerializationUtils.deserialize(value);
        Double currentScore = zSet.getScore(e);
        if (currentScore == null) {
            if ((flags & FLAG_XX) != 0) {
                return null;
            }
            currentScore = incr;
            zSet.add(new AbstractMap.SimpleEntry<>(currentScore, e));
            database.put(key, SerializationUtils.serialize(zSet));
            return currentScore;
        }
        if ((flags & FLAG_NX) != 0) {
            return null;
        }
        if ((flags & FLAG_LT) != 0 && (incr >= 0 || currentScore.isInfinite())) {
            return null;
        }
        if ((flags & FLAG_GT) != 0 && (incr <= 0 || currentScore.isInfinite())) {
            return null;
        }
        zSet.remove(new AbstractMap.SimpleEntry<>(currentScore, e));
        currentScore += incr;
        zSet.add(new AbstractMap.SimpleEntry<>(currentScore, e));
        database.put(key, SerializationUtils.serialize(zSet));
        return currentScore;
    }

    protected int add(final byte[] key, @NonNull final AbstractMap.SimpleEntry<Double, BytesKey>[] members, final int flags) {
        boolean xx = (flags & FLAG_XX) != 0;
        boolean nx = (flags & FLAG_NX) != 0;
        boolean lt = (flags & FLAG_LT) != 0;
        boolean gt = (flags & FLAG_GT) != 0;
        boolean ch = (flags & FLAG_CH) != 0;

        // Track both to eliminate conditional branch
        int added = 0, changed = 0;

        byte[] value = database.get(key);
        ZSet zSet;
        zSet = value == null ? new ZSet() : (ZSet) SerializationUtils.deserialize(value);
        for (AbstractMap.SimpleEntry<Double, BytesKey> member : members) {
            Double currScore = zSet.getScore(member.getValue());
            if (currScore == null) {
                if (xx) {
                    continue;
                }
                currScore = member.getKey();
                zSet.add(new AbstractMap.SimpleEntry<>(currScore, member.getValue()));
                ++added;
                ++changed;
                continue;
            }
            Double newScore = member.getKey();
            if (nx || (lt && newScore >= currScore) || (gt && newScore <= currScore)) {
                continue;
            }
            if (!newScore.equals(currScore)) {
                zSet.removeByKey(member.getValue());
                zSet.add(member);
                ++changed;
            }
        }
        database.put(key, SerializationUtils.serialize(zSet));
        return ch ? changed : added;
    }

    protected Double score(byte[] key, byte[] member) {
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        ZSet zset = (ZSet) SerializationUtils.deserialize(value);
        return zset.getScore(new BytesKey(member));
    }
}
