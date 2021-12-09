package dev.keva.server.command.aof;

import dev.keva.protocol.resp.Command;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AOFOperations {
    public static void write(Command command) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream("keva.aof", true);
        try (ObjectOutputStream output = new ObjectOutputStream(fos)) {
            output.writeObject(command);
        } catch (IOException e) {
            log.error("Error writing to AOF file", e);
        }
    }

    public static List<Command> read() throws IOException {
        try {
            List<Command> commands = new ArrayList<>(100);
            FileInputStream fis = new FileInputStream("keva.aof");
            while (true) {
                try {
                    ObjectInputStream input = new ObjectInputStream(fis);
                    Command command = (Command) input.readObject();
                    commands.add(command);
                } catch (IOException | ClassNotFoundException e) {
                    return commands;
                }
            }
        } catch (FileNotFoundException ignored) {
            return null;
        }
    }
}
