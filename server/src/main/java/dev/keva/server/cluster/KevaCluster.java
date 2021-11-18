package dev.keva.server.cluster;

public interface KevaCluster {

    int getSlotId(byte[] key);

    String forward(int slotId, byte[] command);

    void joinCluster(byte[] slotId, byte[] payload);

    int getCurrentNodeSlotId();
}
