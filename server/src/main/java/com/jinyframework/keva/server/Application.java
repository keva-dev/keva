package com.jinyframework.keva.server;

import com.jinyframework.keva.server.core.Server;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class Application {
    private Application() {
    }

    public static void main(String[] args) {
        try {
            val server = Server.builder()
                    .host("localhost")
                    .port(6767)
                    .heartbeatTimeout(60000)
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
