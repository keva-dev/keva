package dev.keva.core.command.impl.pubsub.manager;

import dev.keva.ioc.annotation.Component;
import io.netty.channel.Channel;
import lombok.Getter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PubSubManager {
    @Getter
    private final ConcurrentMap<String, Set<Channel>> topics = new ConcurrentHashMap<>();

    @Getter
    private final ConcurrentMap<Channel, Set<String>> tracks = new ConcurrentHashMap<>();
}
