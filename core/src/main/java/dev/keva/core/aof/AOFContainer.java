package dev.keva.core.aof;

import dev.keva.core.config.KevaConfig;
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
public class AOFContainer {
    private ReentrantLock bufferLock;
    private ObjectOutputStream output;
    private FileDescriptor fd;
    private boolean alwaysFlush;

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
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
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
    }

    public void write(Command command) {
        bufferLock.lock();
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

    private void flush() throws IOException {
        fd.sync();
    }

    public List<Command> read() throws IOException {
        try {
            List<Command> commands = new ArrayList<>(100);
            FileInputStream fis = new FileInputStream(getWorkingDir() + "keva.aof");
            log.info("AOF size is: {}", fis.getChannel().size());
            ObjectInputStream input = new ObjectInputStream(fis);
            while (true) {
                try {
                    byte[][] objects = (byte[][]) input.readObject();
                    commands.add(Command.newInstance(objects, false));
                } catch (EOFException e) {
                    log.error("Error while reading AOF command", e);
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

    private String getWorkingDir() {
        String workingDir = kevaConfig.getWorkDirectory();
        return workingDir.equals("./") ? "" : workingDir + "/";
    }
}
