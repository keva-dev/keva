package dev.keva.core.command.impl.pubsub;

import dev.keva.ioc.annotation.Autowired;
import dev.keva.ioc.annotation.Component;
import dev.keva.protocol.resp.reply.BulkReply;
import dev.keva.protocol.resp.reply.IntegerReply;
import dev.keva.protocol.resp.reply.MultiBulkReply;
import dev.keva.protocol.resp.reply.Reply;
import dev.keva.core.command.annotation.CommandImpl;
import dev.keva.core.command.annotation.Execute;
import dev.keva.core.command.annotation.ParamLength;
import dev.keva.core.command.impl.pubsub.manager.PubSubManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static dev.keva.core.command.annotation.ParamLength.Type.AT_LEAST;

@Component
@CommandImpl("subscribe")
@ParamLength(type = AT_LEAST, value = 1)
public class Subscribe {
    private final PubSubManager manager;

    @Autowired
    public Subscribe(PubSubManager manager) {
        this.manager = manager;
    }

    @Execute
    public void execute(ChannelHandlerContext ctx, byte[]... topicBytes) {
        val topics = manager.getTopics();
        val tracks = manager.getTracks();

        var track = tracks.get(ctx.channel());
        if (track == null) {
            track = ConcurrentHashMap.newKeySet();
        }

        String[] topicsToSubscribe = new String[topicBytes.length];
        for (int i = 0; i < topicBytes.length; i++) {
            topicsToSubscribe[i] = new String(topicBytes[i]);
        }

        for (val topic : topicsToSubscribe) {
            Set<Channel> list = topics.get(topic);
            if (list == null) {
                list = ConcurrentHashMap.newKeySet();
            }
            list.add(ctx.channel());
            topics.put(topic, list);
            track.add(topic);

            Reply<?>[] replies = new Reply[3];
            replies[0] = new BulkReply("subscribe");
            replies[1] = new BulkReply(topic);
            replies[2] = new IntegerReply(track.size());
            ctx.write(new MultiBulkReply(replies));
        }
    }
}
