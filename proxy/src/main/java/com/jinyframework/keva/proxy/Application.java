package com.jinyframework.keva.proxy;

import com.jinyframework.keva.proxy.config.ConfigHolder;
import com.jinyframework.keva.proxy.config.ConfigManager;
import com.jinyframework.keva.proxy.core.NettyServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class Application {
    public static void main(String[] args) {
        try {
            final ConfigHolder configHolder = ConfigManager.loadConfig(args);
            log.info(configHolder.toString());

            val server = new NettyServer(configHolder);

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
