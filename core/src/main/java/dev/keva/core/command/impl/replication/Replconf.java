package dev.keva.core.command.impl.replication;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.replication.ConnSlaveMap;
import dev.keva.core.replication.ReplConstants;
import dev.keva.core.replication.ReplicationBuffer;
import dev.keva.core.replication.SlaveContext;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.Command;
import dev.keva.protocol.resp.reply.StatusReply;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

import java.util.Locale;
import java.util.concurrent.Executors;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("replconf")
@ParamLength(type = AT_LEAST, value = 1)
@Mutate
public class Replconf {

    private final ConnSlaveMap connSlaveMap;
    private final ReplicationBuffer repBuffer;

    @Autowired
    public Replconf(ConnSlaveMap connSlaveMap, ReplicationBuffer repBuffer) {
        this.connSlaveMap = connSlaveMap;
        this.repBuffer = repBuffer;
    }

    @Execute
    public StatusReply execute(byte[][] args, ChannelHandlerContext ctx) {
        String connKey = SlaveContext.getConnKey(ctx.channel().remoteAddress());
        SlaveContext slaveContext;
        if (connSlaveMap.contains(connKey)) {
            slaveContext = connSlaveMap.get(connKey);
        } else {
            slaveContext = SlaveContext.builder()
                .status(SlaveContext.Status.STARTING)
                .build();
        }

        // REPLCONF ACK offset
        if (new String(args[0]).equalsIgnoreCase("ACK")) {
            slaveContext.setOffset(Long.parseLong(new String(args[1])));

            // mark slave as online
            if (slaveContext.getStatus() == SlaveContext.Status.SYNCING) {
                slaveContext.setStatus(SlaveContext.Status.ONLINE);
            }
            // start sending buffered commands periodically
            Executors.newSingleThreadExecutor().submit(() -> {
                while (slaveContext.getStatus() == SlaveContext.Status.ONLINE) {
                    Command command = repBuffer.peekLast();
                    Jedis jedis = new Jedis(slaveContext.getIpAddress(), Integer.parseInt(slaveContext.getPort()));
                    Protocol.Command jedisCmd = Protocol.Command.valueOf(new String(command.getName()).toUpperCase(Locale.ROOT));
                    jedis.sendCommand(jedisCmd, StringUtils.split(command.toCommandString(false), " "));
                }
            });

            return StatusReply.OK;
        }

        String[] cmdArgs = StringUtils.split(new String(args[0]), " ");
        for (int i = 0; i < cmdArgs.length; i++) {
            String option = cmdArgs[i];
            if (option.equalsIgnoreCase(ReplConstants.IP_ADDRESS)) {
                slaveContext.setIpAddress(cmdArgs[i + 1]);
                continue;
            }
            if (option.equalsIgnoreCase(ReplConstants.LISTENING_PORT)) {
                slaveContext.setPort(cmdArgs[i + 1]);
            }
        }

        connSlaveMap.put(connKey, slaveContext);
        return StatusReply.OK;
    }
}
