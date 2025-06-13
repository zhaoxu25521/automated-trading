package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.MessageDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyHanderMessage extends SimpleChannelInboundHandler<String> {
    private final MessageDispatcher<String> messageDispatcher;

    public NettyHanderMessage(MessageDispatcher<String> messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg)  {
//        log.info("Received WebSocket message: {}", msg.toString(CharsetUtil.UTF_8));
//        String message = msg.text();
        log.info("Received WebSocket message: {}", msg);
//        messageDispatcher.dispatch(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error processing WebSocket message", cause);
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("WebSocket connection established: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("WebSocket connection closed: {}", ctx.channel().remoteAddress());
    }
}
