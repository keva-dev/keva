package com.jinyframework.keva.proxy.balance;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Request {
	private ChannelHandlerContext channelContext;
	private String requestContent = "";
}
