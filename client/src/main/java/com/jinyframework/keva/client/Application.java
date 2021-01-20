package com.jinyframework.keva.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Application {
    public static void main(String[] args) throws Exception {
        KevaClient client = null;
        try {
            client = KevaClient.builder().host("localhost").port(6767).build();
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
