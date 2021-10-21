package dev.keva.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;

@Slf4j
public final class Application {
    public static void main(String[] args) throws Exception {
        KevaClient client = null;
        try {
            val options = new HashMap<String, String>();
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-")) {
                    options.put(args[i].substring(1), args[i + 1]);
                }
            }

            val host = options.getOrDefault("h", "localhost");
            val port = Integer.parseInt(options.getOrDefault("p", "6767"));

            client = KevaClient.builder().host(host).port(port).build();
            client.run();
        } catch (Exception e) {
            log.error("Error occurred while running client: ", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
}
