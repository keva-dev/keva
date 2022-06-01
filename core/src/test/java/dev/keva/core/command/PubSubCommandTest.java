package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PubSubCommandTest extends BaseCommandTest {
    @Test
    @Timeout(30)
    void pubsub() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Thread(() -> subscriber.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                future.complete(message);
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                jedis.publish("test", "Test message");
            }
        }, "test")).start();
        val message = future.get(25, TimeUnit.SECONDS);
        assertEquals("Test message", message);
    }

}
