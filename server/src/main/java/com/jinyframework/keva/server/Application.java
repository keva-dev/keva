package com.jinyframework.keva.server;

import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.ArgsParser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashSet;
import java.util.Map;

@Slf4j
public final class Application {
    private Application() {
    }

    public static Map<String, String> getConfig(String[] args) {
        val options = new HashSet<String>();
        options.add("h");
        options.add("p");
        options.add("ht");
        return ArgsParser.parse(args, options);
    }

    public static void main(String[] args) {
        try {
            val config = getConfig(args);

            val hostname = config.getOrDefault("h", "localhost");
            val port = Integer.parseInt(config.getOrDefault("p", "6767"));
            val heartbeatTimeout = Integer.parseInt(config.getOrDefault("ht", "60000"));

            val server = Server.builder()
                    .host(hostname)
                    .port(port)
                    .heartbeatTimeout(heartbeatTimeout)
                    .build();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.shutdown();
                } catch (Exception e) {
                    log.error("Problem occurred when stopping server: ", e);
                } finally {
                    log.info("Bye");
                }
            }));
            server.run();
        } catch (Exception e) {
            log.error("There was a problem running server: ", e);
        }
    }
}
