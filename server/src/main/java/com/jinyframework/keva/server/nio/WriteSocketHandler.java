package com.jinyframework.keva.server.nio;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

@Slf4j
public class WriteSocketHandler implements CompletionHandler<Integer, Object> {
    private final AsynchronousSocketChannel socketChannel;
    private final ByteBuffer buffer;

    public WriteSocketHandler(AsynchronousSocketChannel socketChannel, ByteBuffer buffer) {
        this.socketChannel = socketChannel;
        this.buffer = buffer;
    }

    @Override
    public void completed(Integer bytesWritten, Object a) {
        buffer.flip();
        final String msg = new String(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        log.info("Sent: {}, bytes: {}", msg, bytesWritten);
    }

    @Override
    public void failed(Throwable throwable, Object a) {

    }
}
