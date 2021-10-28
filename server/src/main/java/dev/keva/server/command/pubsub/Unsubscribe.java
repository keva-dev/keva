package dev.keva.server.command.pubsub;

import dev.keva.server.command.annotation.CommandImpl;
import dev.keva.server.command.annotation.Execute;
import dev.keva.server.protocol.resp.reply.BulkReply;
import dev.keva.server.protocol.resp.reply.IntegerReply;
import dev.keva.server.protocol.resp.reply.MultiBulkReply;
import dev.keva.server.protocol.resp.reply.Reply;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CommandImpl("unsubscribe")
public class Unsubscribe {
    public void remove(ChannelHandlerContext ctx, Set<String> track, String topic) {
        val topics = PubSubFactory.getTopics();
        Set<Channel> list = topics.get(topic);
        if (list != null) {
            list.remove(ctx.channel());
        }
        track.remove(topic);
        Reply<?>[] replies = new Reply[3];
        replies[0] = new BulkReply("unsubscribe");
        replies[1] = new BulkReply(topic);
        replies[2] = new IntegerReply(track.size());
        ctx.write(new MultiBulkReply(replies));
    }

    @Execute
    public void execute(ChannelHandlerContext ctx, byte[]... topicBytes) {
        val tracks = PubSubFactory.getTracks();

        var track = tracks.get(ctx.channel());
        if (track == null) {
            track = ConcurrentHashMap.newKeySet();
        }

        if (topicBytes.length == 0) {
            for (val topic : track) {
                remove(ctx, track, topic);
            }
            return;
        }

        String[] topicsToUnsubscribe = new String[topicBytes.length];
        for (int i = 0; i < topicBytes.length; i++) {
            topicsToUnsubscribe[i] = new String(topicBytes[i]);
        }

        for (val topic : topicsToUnsubscribe) {
            remove(ctx, track, topic);
        }
    }
}
