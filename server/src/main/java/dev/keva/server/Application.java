package dev.keva.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import dev.keva.ioc.KevaIoC;
import dev.keva.server.config.ConfigManager;
import dev.keva.server.config.KevaConfig;
import dev.keva.server.core.KevaServer;
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
            KevaIoC context = KevaIoC.initBeans(Application.class);
            context.getBean(KevaConfig.class).merge(ConfigManager.loadConfig(args));
            context.getBean(KevaServer.class).run();
        } catch (Exception e) {
            log.error("There was a problem running server: ", e);
        }
    }
}
