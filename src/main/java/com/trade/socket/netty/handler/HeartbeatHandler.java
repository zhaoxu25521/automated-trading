package com.trade.socket.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 心跳处理器
 */
public class HeartbeatHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    private final int heartbeatInterval;
    private final String heartbeatMessage;

    public HeartbeatHandler(int heartbeatInterval, String heartbeatMessage) {
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatMessage = heartbeatMessage;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                // 发送心跳消息
                ctx.writeAndFlush(heartbeatMessage);
                logger.debug("Sent heartbeat message: {}", heartbeatMessage);
            } else if (event.state() == IdleState.READER_IDLE) {
                // 读超时，关闭连接
                logger.warn("Heartbeat timeout, closing connection");
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Heartbeat handler exception", cause);
        ctx.close();
    }
}
