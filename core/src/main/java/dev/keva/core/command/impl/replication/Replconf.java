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
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("replconf")
@ParamLength(type = AT_LEAST, value = 1)
@Mutate
@Slf4j
public class Replconf {

    private final ConnSlaveMap connSlaveMap;
    private final ReplicationBuffer repBuffer;

    @Autowired
    public Replconf(ConnSlaveMap connSlaveMap, ReplicationBuffer repBuffer) {
        this.connSlaveMap = connSlaveMap;
        this.repBuffer = repBuffer;
    }

    @Execute
    public Reply<String> execute(byte[][] args, ChannelHandlerContext ctx) {
        String connKey = SlaveContext.getConnKey(ctx.channel().remoteAddress());
        SlaveContext slaveContext;
        if (connSlaveMap.contains(connKey)) {
            slaveContext = connSlaveMap.get(connKey);
        } else {
            slaveContext = SlaveContext.builder()
                .status(SlaveContext.Status.STARTING)
                .build();
        }

        String[] cmdArgs = StringUtils.split(new String(args[0]), " ");
        // REPLCONF ACK offset
        if (cmdArgs[0].equalsIgnoreCase("ACK")) {
            slaveContext.setOffset(Long.parseLong(cmdArgs[1]));

            // mark slave as online
            if (slaveContext.getStatus() == SlaveContext.Status.SYNCING) {
                slaveContext.setStatus(SlaveContext.Status.ONLINE);
            }
            // start sending buffered commands periodically
            new Thread(() -> slaveContext.startForwardCommandJob(repBuffer)).start();
            log.info("Started command forwarding thread for slave {}", slaveContext.slaveName());

            return StatusReply.OK;
        }

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
        log.info("Slave context {}", slaveContext.toString());
        connSlaveMap.put(connKey, slaveContext);
        return StatusReply.OK;
    }
}
