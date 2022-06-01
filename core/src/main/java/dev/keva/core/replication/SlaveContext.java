package dev.keva.core.replication;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Builder
@Getter
@Setter
public class SlaveContext {

    private String ipAddress;
    private String port;
    private long offset;
    private Status status;

    public String slaveName() {
        return ipAddress + ":" + port;
    }

    public static String getConnKey(SocketAddress socketAddress) {
        InetSocketAddress inetAddr = (InetSocketAddress) socketAddress;
        return inetAddr.getAddress().getHostAddress() + ":" + inetAddr.getPort();
    }

    public enum Status {
        ONLINE,
        STARTING,
        SYNCING
    }

}
