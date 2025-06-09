package com.trade.socket.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 消息分发处理器
 * @param <T> 消息类型
 */
public class MessageDispatcher<T> extends SimpleChannelInboundHandler<T> {
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    private final List<MessageHandler<T>> messageHandlers;

    public MessageDispatcher(List<MessageHandler<T>> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
        boolean handled = false;
        for (MessageHandler<T> handler : messageHandlers) {
            if (handler.supports(msg)) {
                try {
                    handler.handle(msg);
                    handled = true;
                } catch (Exception e) {
                    logger.error("Message handling failed", e);
                    handler.handleException(e);
                }
            }
        }

        if (!handled) {
            logger.warn("No handler found for message: {}", msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Message dispatcher error", cause);
        for (MessageHandler<T> handler : messageHandlers) {
            handler.handleException(cause);
        }
        ctx.close();
    }
}
