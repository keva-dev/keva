package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.config.ConfigManager;
import com.jinyframework.keva.server.core.NettyServer;
import com.jinyframework.keva.server.factory.AppFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public final class Application {
    public static void main(String[] args) {
        try {
            final ConfigHolder configHolder = ConfigManager.loadConfig(args);
            AppFactory.setConfigHolder(configHolder);
            log.info(configHolder.toString());

            val server = new NettyServer();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.shutdown();
                } catch (Exception e) {
                    log.error("Problem occurred when stopping server: ", e);
                } finally {
                    log.info("Bye");
                }
            }));
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("#    #                      ######  ######  \n" +
                            "#   #  ###### #    #   ##   #     # #     # \n" +
                            "#  #   #      #    #  #  #  #     # #     # \n" +
                            "###    #####  #    # #    # #     # ######  \n" +
                            "#  #   #      #    # ###### #     # #     # \n" +
                            "#   #  #       #  #  #    # #     # #     # \n" +
                            "#    # ######   ##   #    # ######  ######  ");
                }
            }, 1000);
            server.run();
        } catch (Exception e) {
            log.error("There was a problem running server: ", e);
        }
    }
}
