package com.jinyframework.keva.server.nio;

import com.jinyframework.keva.server.ServiceInstance;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;

@Slf4j
@Deprecated
public class ReadSocketHandler implements CompletionHandler<Integer, StringBuffer> {
    private final AsynchronousSocketChannel socketChannel;
    private final ByteBuffer buffer;

    public ReadSocketHandler(AsynchronousSocketChannel socketChannel, ByteBuffer buffer) {
        this.socketChannel = socketChannel;
        this.buffer = buffer;
    }

    @Override
    public void completed(Integer bytesRead, StringBuffer msgBuf) {
        if (bytesRead == -1) {
            return;
        }
        final String msg = new String(buffer.array(), buffer.arrayOffset(), buffer.position());
        log.info("Received: {}", msg);
        if (msg.indexOf('\n') != -1) {
            final String[] parts = msg.split("\n");
            msgBuf.append(parts[0]);
            // process finished message
            final Object res = ServiceInstance.getCommandService().handleCommand(msgBuf.toString());
            final String resString = String.valueOf(res) + '\n';
            final ByteBuffer responseBuf = ByteBuffer.wrap(resString.getBytes(StandardCharsets.UTF_8));
            socketChannel.write(responseBuf, null, new WriteSocketHandler(socketChannel, responseBuf));

            // make new msgBuf for next message
            msgBuf.delete(0, msgBuf.capacity());
            if (!buffer.hasRemaining() && parts.length > 1 && !parts[1].isBlank()) {
                msgBuf.append(parts[1]);
            }
        } else {
            if (!msg.isBlank()) {
                msgBuf.append(msg);
            }
        }
        buffer.clear();
        socketChannel.read(buffer, msgBuf, new ReadSocketHandler(socketChannel, buffer));
    }

    @Override
    public void failed(Throwable throwable, StringBuffer stringBuffer) {

    }
}
