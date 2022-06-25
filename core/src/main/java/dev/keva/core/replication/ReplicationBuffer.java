package dev.keva.core.replication;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReplicationBuffer {

    @Getter
    private ConcurrentLinkedDeque<Command> buffer;
    @Getter
    private long currentOffset = 0;
    @Getter
    private long startingOffset = 0;
    @Getter
    private long currentSize = 0;
    @Getter
    private long limit; // in bytes
    @Getter
    private long replicationId;

    private final List<ConcurrentLinkedDeque<Command>> slaveBuffers = new ArrayList<>();

    public void init() {
        buffer = new ConcurrentLinkedDeque<>();
        replicationId = System.currentTimeMillis();
        limit = 1024 * 1024; // default to 1 MB
    }

    public void rebase(long replicationId, long startingOffset) {
        this.startingOffset = startingOffset;
        this.currentOffset = startingOffset;
        this.replicationId = replicationId;
        this.currentSize = 0;
        buffer.clear();
    }

    private boolean isWriteCommand(byte[] cmdName) {
        return Arrays.stream(WriteCommand.values()).anyMatch(writeCommand -> writeCommand.name().equalsIgnoreCase(new String(cmdName)));
    }

    public void buffer(Command command) {
        if (!isWriteCommand(command.getName())) {
            return;
        }
        if (currentSize >= limit) {
            Command removed = buffer.removeFirst();
            startingOffset++;
            currentSize = currentSize - removed.getByteSize();
        }
        // need a new instance because the original object will get recycled
        Command cmdInstance = Command.newInstance(command.getObjects(), false);
        buffer.add(cmdInstance);
        currentOffset++;
        currentSize += currentSize + command.getByteSize();
        slaveBuffers.forEach(buf -> {
            buf.addLast(cmdInstance);
        });
        log.trace(Arrays.toString(buffer.toArray()));
    }

    public ArrayList<String> dump() {
        return buffer.stream().map(cmd -> cmd.toCommandString(true))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public void register(ConcurrentLinkedDeque<Command> slaveBuffer) {
        slaveBuffers.add(slaveBuffer);
    }

}
