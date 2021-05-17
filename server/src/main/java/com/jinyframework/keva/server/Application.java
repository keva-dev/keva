package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.config.ConfigManager;
import com.jinyframework.keva.server.core.Server;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class Application {
    private Application() {
    }

    public static void main(String[] args) {
        try {
            final ConfigHolder configHolder = ConfigManager.loadConfig(args);
            log.info(configHolder.toString());

            val server = new Server(configHolder);

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
