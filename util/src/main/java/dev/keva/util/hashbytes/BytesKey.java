package dev.keva.util.hashbytes;

import com.google.common.primitives.SignedBytes;

import java.io.Serializable;
import java.util.Comparator;

public class BytesKey extends BytesValue implements Comparable<BytesKey>, Serializable {
    private static final Comparator<byte[]> COMPARATOR = SignedBytes.lexicographicalComparator();

    private final int hashCode;

    public BytesKey(byte[] bytes) {
        super(bytes);
        int hashCode = 0;
        for (byte aByte : this.bytes) {
            hashCode += 43 * aByte;
        }
        this.hashCode = hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BytesKey) {
            BytesKey other = (BytesKey) o;
            return hashCode == other.hashCode && equals(bytes, other.bytes);
        }
        return false;
    }

    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(BytesKey o) {
        return COMPARATOR.compare(this.bytes, o.bytes);
    }
}
