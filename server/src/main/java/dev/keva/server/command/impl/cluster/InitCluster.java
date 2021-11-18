package dev.keva.server.command.impl.cluster;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.StatusReply;
import dev.keva.server.cluster.KevaCluster;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import lombok.val;

import java.lang.management.ManagementFactory;

import static dev.keva.server.command.annotation.ParamLength.Type.EXACT;

@Component
@CommandImpl("initcluster")
@ParamLength(type = EXACT, value = 2)
public class InitCluster {

    private final KevaCluster kevaCluster;

    @Autowired
    public InitCluster(KevaCluster kevaCluster) {
        this.kevaCluster = kevaCluster;
    }

    @Execute
    public StatusReply execute(byte[] slotId, byte[] payload) {
        kevaCluster.joinCluster(slotId, payload);
        return StatusReply.OK;
    }
}
