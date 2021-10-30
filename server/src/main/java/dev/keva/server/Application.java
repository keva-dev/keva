package dev.keva.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import dev.keva.server.config.ConfigManager;
import dev.keva.server.core.AppFactory;
import dev.keva.server.core.KevaDB;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.LoggerFactory;

@Slf4j
public final class Application {
    static {
        val ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger("io.netty").setLevel(Level.OFF);
        ctx.getLogger("net.openhft").setLevel(Level.OFF);
    }

    public static void main(String[] args) {
        try {
            AppFactory.setConfig(ConfigManager.loadConfig(args));
            new KevaDB().run();
        } catch (Exception e) {
            log.error("There was a problem running server: ", e);
        }
    }
}
