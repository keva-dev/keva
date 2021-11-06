package dev.keva.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.keva.config.ConfigLoader;
import dev.keva.server.core.KevaServer;
import dev.keva.server.config.KevaConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.LoggerFactory;

@Slf4j
public final class Application {
    static {
        val ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger("io.netty").setLevel(Level.OFF);
        ctx.getLogger("net.openhft").setLevel(Level.OFF);
        ctx.getLogger("org.reflections").setLevel(Level.OFF);
        System.err.close();
    }

    public static void main(String[] args) {
        try {
            val config = ConfigLoader.loadConfig(args, KevaConfig.class);
            val server = KevaServer.of(config);
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
            server.run();
        } catch (Exception e) {
            log.error("There was a problem running server: ", e);
        }
    }
}
