package dev.keva.core.aof;

import dev.keva.core.config.KevaConfig;
import dev.keva.core.exception.StartupException;
import dev.keva.ioc.annotation.Autowired;
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
public class AOFContainer implements Closeable {
    private ReentrantLock bufferLock;
    private ObjectOutputStream output;
    private FileDescriptor fd;
    private boolean alwaysFlush;
    private ScheduledExecutorService executorService;
    private volatile boolean isOpen;

    @Autowired
    private KevaConfig kevaConfig;

    public void init() {
        alwaysFlush = kevaConfig.getAofInterval() == 0;
        bufferLock = new ReentrantLock();

        try {
            FileOutputStream fos = new FileOutputStream(getWorkingDir() + "keva.aof", true);
            fd = fos.getFD();
            output = new ObjectOutputStream(fos);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                log.info("AOF file not found, creating new file...");
                if (e.getMessage().contains("Permission denied")) {
                    log.error("Permission denied to access AOF file, please check your file permissions");
                    System.exit(1);
                }
            } else {
                log.error("Error writing to AOF file", e);
                System.exit(1);
            }
        }

        if (!alwaysFlush) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(() -> {
                try {
                    flush();
                } catch (IOException e) {
                    log.error("Error writing AOF file", e);
                }
            }, kevaConfig.getAofInterval(), kevaConfig.getAofInterval(), TimeUnit.MILLISECONDS);
            log.info("AOF started with interval {} ms", kevaConfig.getAofInterval());
        } else {
            log.info("AOF will trigger for every new mutate command");
        }
        isOpen = true;
    }

    public void write(Command command) {
        bufferLock.lock();
        if (!isOpen) {
            log.warn("Dropping write to AOF as it is closed!");
            return;
        }
        try {
            output.writeObject(command.getObjects());
            if (alwaysFlush) {
                flush();
            }
        } catch (IOException e) {
            log.error("Error writing AOF file", e);
        } finally {
            bufferLock.unlock();
        }
    }

    public List<Command> read() throws IOException {
        final List<Command> commands = new ArrayList<>(100);
        try (FileInputStream fis = new FileInputStream(getWorkingDir() + "keva.aof");
             ObjectInputStream input = new ObjectInputStream(fis)) {
            log.info("AOF size is: {}", fis.getChannel().size());
            while (true) {
                byte[][] objects = (byte[][]) input.readObject();
                commands.add(Command.newInstance(objects, false));
            }
        } catch (final FileNotFoundException | EOFException ignored) {
            return commands;
        } catch (final ClassNotFoundException e) {
            final String msg = "Error reading AOF file";
            log.error(msg, e);
            throw new StartupException(msg, e);
        }
    }

    public void close() throws IOException {
        bufferLock.lock();
        isOpen = false;
        log.info("Closing AOF log.");
        try {
            if (executorService != null) {
                executorService.shutdown();
            }
            // Closing the stream should flush it, but still doing it explicitly!
            flush();
            output.close();
        } finally {
            bufferLock.unlock();
        }
    }

    private void flush() throws IOException {
        fd.sync();
    }

    private String getWorkingDir() {
        String workingDir = kevaConfig.getWorkDirectory();
        return workingDir.equals("./") ? "" : workingDir + "/";
    }
}
