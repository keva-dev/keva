package dev.keva.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import dev.keva.server.config.ConfigHolder;
import dev.keva.server.config.ConfigManager;
import dev.keva.server.core.NettyServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public final class Application {
    static {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger("io.netty").setLevel(Level.OFF);
        ctx.getLogger("net.openhft").setLevel(Level.OFF);
    }

    public static void main(String[] args) {
        try {
            final ConfigHolder configHolder = ConfigManager.loadConfig(args);
            log.info("Configuration loaded");
            log.info(configHolder.toString());
            val server = new NettyServer(configHolder);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.shutdown();
                } catch (Exception e) {
                    log.error("Problem occurred when stopping server: ", e);
                } finally {
                    log.info("Bye!");
                }
            }));

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("\n" +
                            "      ___           \n" +
                            "|__/ |__  \\  /  /\\  \n" +
                            "|  \\ |___  \\/  /~~\\ \n" +
                            "                    \n");
                }
            }, 1000);

            server.run();
        } catch (Exception e) {
            log.error("There was a problem running server: ", e);
        }
    }
}
