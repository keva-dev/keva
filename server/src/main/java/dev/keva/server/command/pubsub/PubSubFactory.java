package dev.keva.server.command.pubsub;

import io.netty.channel.Channel;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PubSubFactory {
    @Getter
    private static final ConcurrentMap<String, Set<Channel>> topics = new ConcurrentHashMap<>();

    @Getter
    private static final ConcurrentMap<Channel, Set<String>> tracks = new ConcurrentHashMap<>();
}
