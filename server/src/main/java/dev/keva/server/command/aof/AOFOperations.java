package dev.keva.server.command.aof;

import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class AOFOperations {
    private final ReentrantLock bufferLock = new ReentrantLock();
    private List<Command> buffer;
    private ObjectOutputStream output;

    public void init() {
        buffer = new ArrayList<>(100);

        try {
            boolean isExists = new File("keva.aof").exists();
            FileOutputStream fos = new FileOutputStream("keva.aof", true);
            output = isExists ? new AppendOnlyObjectOutputStream(fos) : new ObjectOutputStream(fos);
        } catch (IOException e) {
            log.error("Error creating AOF file", e);
        }
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                sync();
            } catch (IOException e) {
                log.error("Error syncing AOF file", e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // Should be configurable interval
    }

    public void write(Command command) {
        bufferLock.lock();
        try {
            buffer.add(command);
        } finally {
            bufferLock.unlock();
        }
    }

    public void sync() throws IOException {
        if (buffer.isEmpty()) {
            return;
        }
        bufferLock.lock();
        try {
            for (Command command : buffer) {
                output.writeObject(command);
            }
        } finally {
            output.flush();
            buffer.clear();
            bufferLock.unlock();
        }
    }

    public List<Command> read() throws IOException {
        try {
            List<Command> commands = new ArrayList<>(100);
            FileInputStream fis = new FileInputStream("keva.aof");
            ObjectInputStream input = new ObjectInputStream(fis);
            while (true) {
                try {
                    Command command = (Command) input.readObject();
                    commands.add(command);
                } catch (EOFException e) {
                    fis.close();
                    return commands;
                } catch (ClassNotFoundException e) {
                    log.error("Error reading AOF file", e);
                    return commands;
                }
            }
        } catch (FileNotFoundException ignored) {
            throw new FileNotFoundException("AOF file not found");
        }
    }
}
