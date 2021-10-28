package dev.keva.server.command.pubsub;

import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.command.annotation.ParamLength;
import dev.keva.server.protocol.resp.reply.BulkReply;
import dev.keva.server.protocol.resp.reply.IntegerReply;
import dev.keva.server.protocol.resp.reply.MultiBulkReply;
import dev.keva.server.protocol.resp.reply.Reply;
import io.netty.channel.Channel;
import lombok.val;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@CommandImpl("publish")
@ParamLength(2)
public class Publish {
    private final ConcurrentMap<String, Set<Channel>> topics = PubSubFactory.getTopics();

    @Execute
    public IntegerReply execute(byte[] topicBytes, byte[] message) {
        var count = 0;
        val topic = new String(topicBytes).toLowerCase();
        Set<Channel> set = topics.get(topic);
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
