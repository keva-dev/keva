package dev.keva.server.cluster;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.server.config.KevaConfig;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;

@Component
public class KevaClusterImpl implements KevaCluster {

    private int currentSlotId;

    private final ConcurrentSkipListMap<Integer, Integer> nodes = new ConcurrentSkipListMap<>();

    private final HashMap<Integer, KevaNode> connections = new HashMap<>();

    private KevaConfig kevaConfig;

    @Autowired
    public KevaClusterImpl(KevaConfig kevaConfig) {
        this.kevaConfig = kevaConfig;
    }

    @Override
    public int getSlotId(byte[] key) {
        ConcurrentNavigableMap<Integer, Integer> subMap = nodes.tailMap(Arrays.hashCode(key));
        int slotId;
        if (subMap.isEmpty()) {
            //If there is no one larger than the hash value of the key, start with the first node
            slotId = nodes.firstKey();
        } else {
            //The first Key is the nearest node clockwise past the node.
            slotId = subMap.firstKey();
        }
        return slotId;
    }

    @Override
    public int getCurrentNodeSlotId() {
        return currentSlotId;
    }

    @Override
    public String forward(int slotId, byte[] command) {
        String reply = "";
        try {
            reply = connections.get(slotId).send(command);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return reply;
    }

    @Override
    public void joinCluster(byte[] slotId, byte[] payload) {
        kevaConfig.setCluster(true);
        this.currentSlotId = ByteBuffer.wrap(slotId).getInt();
        for (String node : ByteBuffer.wrap(payload).toString().split(";")) {
            val values = node.split(":");
            connections.put(Integer.parseInt(values[0]), new KevaNode(values[1], values[2]));
        }
    }
}
