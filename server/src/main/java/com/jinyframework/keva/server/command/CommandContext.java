package com.jinyframework.keva.server.command;

import lombok.Builder;
import lombok.Data;

import java.net.SocketAddress;

@Builder
@Data
public class CommandContext {
    private final SocketAddress remoteAddr;
}
