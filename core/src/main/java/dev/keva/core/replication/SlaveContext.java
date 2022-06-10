package dev.keva.core.replication;

import dev.keva.protocol.resp.Command;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;

@Builder
@Data
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

    public void startForwardCommandJob(ReplicationBuffer repBuffer) {
        ConcurrentLinkedDeque<Command> slaveBuffer = new ConcurrentLinkedDeque<>(repBuffer.getBuffer());
        repBuffer.register(slaveBuffer);
        Jedis jedis = new Jedis(this.getIpAddress(), Integer.parseInt(this.getPort()), 120000);
        while (this.getStatus() == SlaveContext.Status.ONLINE) {
            if (slaveBuffer.isEmpty()) {
                continue;
            }
            Command command = slaveBuffer.removeFirst();
            Protocol.Command jedisCmd = Protocol.Command.valueOf(new String(command.getName()).toUpperCase(Locale.ROOT));
            jedis.sendCommand(jedisCmd, StringUtils.split(command.toCommandString(false), " "));
        }
    }

    public enum Status {
        ONLINE,
        STARTING,
        SYNCING
    }

}
