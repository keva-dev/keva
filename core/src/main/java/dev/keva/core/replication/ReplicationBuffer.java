package dev.keva.core.replication;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.Stream;

@Component
@Slf4j
public class ReplicationBuffer {

    private Deque<Command> buffer;
    @Getter
    private long currentOffset = 0;
    @Getter
    private long startingOffset = 0;
    @Getter
    private long capacity = 0;
    @Getter
    private long limit; // in bytes

    private final Stream<WriteCommand> writeCommandStream = Arrays.stream(WriteCommand.values());

    public void init() {
        buffer = new ArrayDeque<>();
        limit = 1024 * 1024; // default to 1 MB
    }

    private boolean isWriteCommand(byte[] cmdName) {
        return writeCommandStream.anyMatch(writeCommand -> Arrays.equals(writeCommand.getRaw(), cmdName));
    }

    public void buffer(Command command) {
        if (!isWriteCommand(command.getName())) {
            return;
        }
        if (capacity >= limit) {
            Command removed = buffer.removeFirst();
            startingOffset++;
            capacity = capacity - removed.getByteSize();
        }
        // need a new instance because the original object will get recycled
        buffer.addLast(Command.newInstance(command.getObjects(), false));
        currentOffset++;
        capacity += capacity + command.getByteSize();
        log.trace(Arrays.toString(buffer.toArray()));
    }

}
