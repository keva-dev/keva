package dev.keva.core.command.impl.replication;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.Mutate;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.config.KevaConfig;
import dev.keva.core.replication.ReplicationBuffer;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.store.KevaDatabase;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;

@Component
@CommandImpl("psync")
@ParamLength(2)
@Mutate
public class PSYNC {
    private final KevaDatabase database;
    private final KevaConfig kevaConfig;
    private final ReplicationBuffer repBuffer;
    private final String snapshotFilePath;

    @Autowired
    public PSYNC(KevaDatabase database, KevaConfig kevaConfig, ReplicationBuffer repBuffer) {
        this.database = database;
        this.kevaConfig = kevaConfig;
        this.repBuffer = repBuffer;
        this.snapshotFilePath = kevaConfig.getWorkDirectory() + "/temp.kdb";
    }

    @Execute
    public MultiBulkReply execute(ChannelHandlerContext ctx,
                                  byte[] replicationId,
                                  byte[] startingOffset) {
        // PSYNC replicationId startingOffset
        String repId = new String(replicationId);
        long slaveStartingOffset = Long.parseLong(new String(startingOffset));
        boolean needFullResync = repBuffer.getStartingOffset() > slaveStartingOffset;
        ArrayList<String> cmdList = repBuffer.dump();
        if (needFullResync) {
            ctx.write(new BulkReply("FULLRESYNC " + repBuffer.getReplicationId() + " " + repBuffer.getStartingOffset()));

            // generate snapshot
            generateSnapshot();

            // send snapshot
            String snapshotFileAsString = readSnapshotFileToString();
            ctx.write(new BulkReply(snapshotFileAsString));

            // register slave
            // start long connection with slave to forward command afterwards

            // send buffered commands
            ctx.write(new BulkReply(cmdList.toString()));
        } else {
            ctx.write(new StatusReply("CONTINUE"));
            ctx.write(new BulkReply(cmdList.toString()));
        }

        return null;
    }

    private String readSnapshotFileToString() {
        return "";
    }

    private void generateSnapshot() {
        // https://github.com/keva-dev/keva-dbutil/blob/master/src/main/java/dev/keva/dbutil/Converter.java

        // load chroniclemap in new thread

        // serialize to snapshot file
    }

}
