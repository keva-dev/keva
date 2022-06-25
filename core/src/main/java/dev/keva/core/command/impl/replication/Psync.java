package dev.keva.core.command.impl.replication;

import com.google.common.io.Files;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.config.KevaConfig;
import dev.keva.core.replication.ConnSlaveMap;
import dev.keva.core.replication.ReplicationBuffer;
import dev.keva.core.replication.SlaveContext;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.protocol.resp.reply.StatusReply;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;

import java.io.File;
import java.util.ArrayList;

import static dev.keva.core.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("psync")
@ParamLength(type = EXACT, value = 2)
@Mutate
public class Psync {

    private final ReplicationBuffer repBuffer;
    private final String persistenceFilePath;
    private final ConnSlaveMap connSlaveMap;

    @Autowired
    public Psync(KevaConfig kevaConfig, ReplicationBuffer repBuffer, ConnSlaveMap connSlaveMap) {
        this.repBuffer = repBuffer;
        this.persistenceFilePath = kevaConfig.getWorkDirectory() + "/dump.kdb";
        this.connSlaveMap = connSlaveMap;
    }

    @Execute
    public StatusReply execute(byte[] replicationId,
                               byte[] startingOffset,
                               ChannelHandlerContext ctx) {
        // PSYNC replicationId startingOffset
        String repId = new String(replicationId);
        String masterRepId = String.valueOf(repBuffer.getReplicationId());
        long slaveStartingOffset = Long.parseLong(new String(startingOffset));
        boolean needFullResync = !masterRepId.equalsIgnoreCase(repId) || repBuffer.getStartingOffset() > slaveStartingOffset;
        ArrayList<String> cmdList = repBuffer.dump();

        SlaveContext slaveContext = connSlaveMap.get(SlaveContext.getConnKey(ctx.channel().remoteAddress()));
        slaveContext.setStatus(SlaveContext.Status.SYNCING);

        if (needFullResync) {
            ctx.write(new BulkReply("FULLRESYNC " + repBuffer.getReplicationId() + " " + repBuffer.getStartingOffset()));

            // send snapshot
            ctx.write(new BulkReply(readSnapshotFileToString()));

            // send buffered commands
            Reply<?>[] cmdReplies = new Reply[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                String cmd = cmdList.get(i);
                cmdReplies[i] = new BulkReply(cmd);
            }
            ctx.write(new MultiBulkReply(cmdReplies));
        } else {
            ctx.write(new StatusReply("CONTINUE"));
            ctx.write(new BulkReply(cmdList.toString()));
        }

        return StatusReply.OK;
    }

    @SneakyThrows
    private byte[] readSnapshotFileToString() {
        return Files.asByteSource(new File(persistenceFilePath)).read();
    }

}
