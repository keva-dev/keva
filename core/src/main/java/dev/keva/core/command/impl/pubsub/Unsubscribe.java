package dev.keva.core.command.impl.pubsub;

import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.impl.pubsub.manager.PubSubManager;
import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@CommandImpl("unsubscribe")
public class Unsubscribe {
    private final PubSubManager manager;

    @Autowired
    public Unsubscribe(PubSubManager manager) {
        this.manager = manager;
    }

    public void remove(ChannelHandlerContext ctx, Set<String> track, String topic) {
        Map<String, Set<Channel>> topics = manager.getTopics();
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
        Map<Channel, Set<String>> tracks = manager.getTracks();

        Set<String> track = tracks.get(ctx.channel());
        if (track == null) {
            track = ConcurrentHashMap.newKeySet();
        }

        if (topicBytes.length == 0) {
            for (String topic : track) {
                remove(ctx, track, topic);
            }
            return;
        }

        String[] topicsToUnsubscribe = new String[topicBytes.length];
        for (int i = 0; i < topicBytes.length; i++) {
            topicsToUnsubscribe[i] = new String(topicBytes[i]);
        }

        for (String topic : topicsToUnsubscribe) {
            remove(ctx, track, topic);
        }
    }
}
