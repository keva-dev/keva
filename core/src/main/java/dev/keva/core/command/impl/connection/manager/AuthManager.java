package dev.keva.core.command.impl.connection.manager;

import dev.keva.ioc.annotation.Component;
import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AuthManager {
    private final ConcurrentMap<Channel, Boolean> auths = new ConcurrentHashMap<>();

    public boolean isAuthenticated(Channel channel) {
        return auths.get(channel) != null && auths.get(channel);
    }

    public void authenticate(Channel channel) {
        auths.put(channel, true);
    }

    public void unAuthenticate(Channel channel) {
        auths.remove(channel);
    }
}
