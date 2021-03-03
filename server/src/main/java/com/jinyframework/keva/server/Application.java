package com.jinyframework.keva.server;

import com.jinyframework.keva.server.config.ConfigHolder;
import com.jinyframework.keva.server.config.ConfigManager;
import com.jinyframework.keva.server.core.Server;
import com.jinyframework.keva.server.util.ArgsParser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Properties;

@Slf4j
public final class Application {
    private Application() {
    }

    public static void main(String[] args) {
        try {
            val config = ArgsParser.parse(args);
            log.info(config.toString());
            val overrider = ConfigHolder.fromArgs(config);

            val configFilePath = config.getArgVal("f");
            if (configFilePath != null) {
                ConfigManager.loadConfigFromFile(configFilePath);
            } else {
                // init using cli args values
                ConfigManager.setConfig(ConfigHolder.fromProperties(new Properties()));
            }

            ConfigManager.getConfig().merge(overrider);

            log.info(ConfigManager.getConfig().toString());
            val server = new Server(ConfigManager.getConfig());

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
