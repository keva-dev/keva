package dev.keva.server.command.pubsub;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.command.pubsub.manager.PubSubManager;
import io.netty.channel.Channel;
import lombok.val;

import java.util.Set;

@Component
@CommandImpl("publish")
@ParamLength(2)
public class Publish {
    @Autowired
    private PubSubManager manager;

    @Execute
    public IntegerReply execute(byte[] topicBytes, byte[] message) {
        var count = 0;
        val topic = new String(topicBytes).toLowerCase();
        Set<Channel> set = manager.getTopics().get(topic);
        if (set != null) {
            for (Channel channel : set) {
                if (channel.isActive()) {
                    Reply<?>[] replies = new Reply[3];
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
