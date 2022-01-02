package dev.keva.core.command.impl.pubsub;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.pubsub.manager.PubSubManager;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import io.netty.channel.Channel;

import java.util.Set;

@Component
@CommandImpl("publish")
@ParamLength(2)
public class Publish {
    private final PubSubManager manager;

    @Autowired
    public Publish(PubSubManager manager) {
        this.manager = manager;
    }

    @Execute
    public IntegerReply execute(byte[] topicBytes, byte[] message) {
        int count = 0;
        String topic = new String(topicBytes).toLowerCase();
        Set<Channel> set = manager.getTopics().get(topic);
        if (set != null) {
            for (Channel channel : set) {
                if (channel.isActive()) {
                    BulkReply[] replies = new BulkReply[3];
                    replies[0] = new BulkReply("message");
                    replies[1] = new BulkReply(topic);
                    replies[2] = new BulkReply(message);
                    channel.writeAndFlush(new MultiBulkReply(replies));
                    count++;
                } else {
                    if (!channel.isOpen()) {
                        set.remove(channel);
                    }
                }
            }
        }
        return new IntegerReply(count);
    }
}
