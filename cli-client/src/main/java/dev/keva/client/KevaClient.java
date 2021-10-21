package dev.keva.client;

import lombok.Builder;
import lombok.val;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

@Builder
public class KevaClient {
    private final String host;
    private final int port;
    private PrintWriter socketOut;
    private Socket socket;
    private BufferedReader socketIn;
    private BufferedReader consoleIn;
    private final PrintStream console = System.out;

    public void init() throws Exception {
        consoleIn = new BufferedReader(new InputStreamReader(System.in));
        socket = new Socket(host, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socketOut = new PrintWriter(socket.getOutputStream());
        console.println("Client Started");
        console.println("Press 'q' to quit client");
    }

    public void run() throws Exception {
        init();
        while (!socket.isClosed()) {
            console.print(socket.getRemoteSocketAddress() + "> ");
            val consoleLine = consoleIn.readLine();
            if (consoleLine == null || consoleLine.isEmpty()) {
                continue;
            }

            if ("q".equals(consoleLine)) {
                break;
            }
            socketOut.println(consoleLine);
            socketOut.flush();
            val line = socketIn.readLine();
            if (line == null) {
                console.println("Lost connection to server");
                break;
            }
            console.println(line);
        }
    }

    public void shutdown() throws Exception {
        consoleIn.close();
        socketIn.close();
        socketOut.close();
        socket.close();
        console.println("Bye");
    }
}
