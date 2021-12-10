package dev.keva.core.server;

import io.netty.channel.ServerChannel;
import io.netty.util.concurrent.AbstractEventExecutorGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyNativeTransportLoader {
    @Getter
    private static Class<? extends AbstractEventExecutorGroup> eventExecutorGroupClazz;
    @Getter
    private static Class<? extends ServerChannel> serverSocketChannelClazz;

    static {
        Platform os = detectPlatformOS();
        boolean result;
        if (os == Platform.WINDOWS || os == Platform.SOLARIS || os == Platform.UNIX || os == Platform.UNKNOWN) {
            result = loadDefault();
            log.info("Loaded default library for {}", os);
        } else if (os == Platform.LINUX) {
            result = loadEpoll();
            log.info("Loaded epoll native library for {}", os);
        } else {
            result = loadKqueue();
            log.info("Loaded kqueue native library for {}", os);
        }
        if (!result) {
            log.error("Failed to load library for Netty");
            System.exit(1);
        }
    }

    public static Platform detectPlatformOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return Platform.WINDOWS;
        } else if (osName.contains("mac")) {
            return Platform.MAC;
        } else if (osName.contains("solaris") || osName.contains("sunos")) {
            return Platform.SOLARIS;
        } else if (osName.contains("linux")) {
            return Platform.LINUX;
        } else if (osName.contains("unix")) {
            return Platform.UNIX;
        } else {
            return Platform.UNKNOWN;
        }
    }

    public static boolean loadDefault() {
        try {
            eventExecutorGroupClazz = Class.forName("io.netty.channel.nio.NioEventLoopGroup").asSubclass(AbstractEventExecutorGroup.class);
            serverSocketChannelClazz = Class.forName("io.netty.channel.socket.nio.NioServerSocketChannel").asSubclass(ServerChannel.class);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean loadKqueue() {
        try {
            eventExecutorGroupClazz = Class.forName("io.netty.channel.kqueue.KQueueEventLoopGroup").asSubclass(AbstractEventExecutorGroup.class);
            serverSocketChannelClazz = Class.forName("io.netty.channel.kqueue.KQueueServerSocketChannel").asSubclass(ServerChannel.class);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean loadEpoll() {
        try {
            eventExecutorGroupClazz = Class.forName("io.netty.channel.epoll.EpollEventLoopGroup").asSubclass(AbstractEventExecutorGroup.class);
            serverSocketChannelClazz = Class.forName("io.netty.channel.epoll.EpollServerSocketChannel").asSubclass(ServerChannel.class);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public enum Platform {
        LINUX, MAC, WINDOWS, SOLARIS, UNIX, UNKNOWN
    }

    public static class NettyNativeLoaderException extends Exception {
        public NettyNativeLoaderException(String message) {
            super(message);
        }
    }
}
