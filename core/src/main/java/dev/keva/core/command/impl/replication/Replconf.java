package dev.keva.core.command.impl.replication;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.replication.ConnSlaveMap;
import dev.keva.core.replication.ReplConstants;
import dev.keva.core.replication.SlaveContext;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.StatusReply;
import io.netty.channel.ChannelHandlerContext;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("replconf")
@ParamLength(type = AT_LEAST, value = 1)
@Mutate
public class Replconf {

    private final ConnSlaveMap connSlaveMap;

    @Autowired
    public Replconf(ConnSlaveMap connSlaveMap) {
        this.connSlaveMap = connSlaveMap;
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
            // start sending buffered commands

            return StatusReply.OK;
        }

        for (int i = 0; i < args.length; i++) {
            String option = new String(args[i]);
            if (option.equalsIgnoreCase(ReplConstants.IP_ADDRESS)) {
                slaveContext.setIpAddress(new String(args[i+1]));
                continue;
            }
            if (option.equalsIgnoreCase(ReplConstants.LISTENING_PORT)) {
                slaveContext.setPort(new String(args[i+1]));
            }
        }

        return StatusReply.OK;
    }
}
