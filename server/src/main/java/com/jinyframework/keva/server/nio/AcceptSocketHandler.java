package com.jinyframework.keva.server.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

@Slf4j
public class AcceptSocketHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
    private final AsynchronousServerSocketChannel server;

    public AcceptSocketHandler(AsynchronousServerSocketChannel server) {
        this.server = server;
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, Object a) {
        server.accept(null, this);
        try {
            log.info("{} connected", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            log.warn("Failed to log remote address", e);
        }

        final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        socketChannel.read(buffer, new StringBuffer(), new ReadSocketHandler(socketChannel, buffer));
    }

    @Override
    public void failed(Throwable throwable, Object a) {

    }
}
