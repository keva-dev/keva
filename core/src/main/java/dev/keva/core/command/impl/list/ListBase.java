package dev.keva.core.command.impl.list;

import dev.keva.storage.KevaDatabase;
import dev.keva.util.hashbytes.BytesValue;
import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class ListBase {
    protected final KevaDatabase database;

    public ListBase(KevaDatabase database) {
        this.database = database;
    }

    protected LinkedList<BytesValue> getList(byte[] key) {
        byte[] value = database.get(key);
        if (value == null) {
            return null;
        }
        return (LinkedList<BytesValue>) SerializationUtils.deserialize(value);
    }

    protected byte[] lpop(byte[] key) {
        LinkedList<BytesValue> list = getList(key);
        if (list.isEmpty()) {
            return null;
        }
        byte[] result = list.removeFirst().getBytes();
        database.put(key, SerializationUtils.serialize(list));
        return result;
    }

    protected byte[] rpop(byte[] key) {
        LinkedList<BytesValue> list = this.getList(key);
        if (list.isEmpty()) {
            return null;
        }
        byte[] result = list.removeLast().getBytes();
        database.put(key, SerializationUtils.serialize(list));
        return result;
    }

    protected int lpush(byte[] key, byte[]... values) {
        LinkedList<BytesValue> list = getList(key);
        if (list == null) {
            list = new LinkedList<>();
        }
        for (byte[] v : values) {
            list.addFirst(new BytesValue(v));
        }
        database.put(key, SerializationUtils.serialize(list));
        return list.size();
    }

    protected int rpush(byte[] key, byte[]... values) {
        byte[] value = database.get(key);
        LinkedList<BytesValue> list;
        list = value == null ? new LinkedList<>() : this.getList(key);
        for (byte[] v : values) {
            list.addLast(new BytesValue(v));
        }
        database.put(key, SerializationUtils.serialize(list));
        return list.size();
    }

    protected void set(byte[] key, int index, byte[] value) {
        LinkedList<BytesValue> list = this.getList(key);
        if (index < 0) {
            index = list.size() + index;
        }
        if (index < 0 || index >= list.size()) {
            return;
        }
        list.set(index, new BytesValue(value));
        database.put(key, SerializationUtils.serialize(list));
    }

    protected byte[][] range(byte[] key, int start, int end) {
        LinkedList<BytesValue> list = this.getList(key);
        int size = list.size();
        if (start < 0) {
            start = size + start;
        }
        if (end < 0) {
            end = size + end;
        }
        if (start < 0) {
            start = 0;
        }
        if (end > size) {
            end = size;
        }
        if (start > end) {
            return null;
        }
        List<byte[]> result = new ArrayList<>(0);
        for (int j = start; j <= end; j++) {
            try {
                if (list.get(j) != null) {
                    result.add(list.get(j).getBytes());
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return result.toArray(new byte[0][0]);
    }

    protected int remove(byte[] key, int count, byte[] value) {
        LinkedList<BytesValue> list = this.getList(key);
        int size = list.size();
        int result = 0;
        if (count > 0) {
            for (int i = 0; i < size; i++) {
                if (Arrays.equals(list.get(i).getBytes(), value)) {
                    if (count != 0) {
                        count--;
                        list.remove(i);
                        result++;
                        size--;
                    }
                }
            }
        } else if (count < 0) {
            for (int i = size - 1; i >= 0; i--) {
                if (Arrays.equals(list.get(i).getBytes(), value)) {
                    if (count != 0) {
                        count++;
                        list.remove(i);
                        result++;
                        size--;
                    }
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (Arrays.equals(list.get(i).getBytes(), value)) {
                    list.remove(i);
                    result++;
                    size--;
                }
            }
        }
        database.put(key, SerializationUtils.serialize(list));
        return result;
    }
}
