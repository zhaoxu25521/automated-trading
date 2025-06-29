package com.trade.socket.netty.handler;

import com.trade.socket.netty.client.NettyClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息分发器
 * @param <T> 消息类型
 */
@Slf4j
public class MessageDispatcher<T> {
    private final List<MessageHandler<T>> handlers;
    private NettyClient client;

    public MessageDispatcher(List<MessageHandler<T>> handlers, NettyClient client) {
        this.handlers = handlers;
        this.client = client;
    }

    public MessageDispatcher(NettyClient client) {
        this.handlers = new ArrayList<>();
        this.client = client;
    }

    public void setClient(NettyClient client) {
        this.client = client;
    }

    /**
     * 分发消息给处理器链
     * @param message 要处理的消息
     */
    public void dispatch(T message) {
        if (client == null) {
            log.error("Client is not set in MessageDispatcher");
            return;
        }
        
        MessageHandler.HandlerContext ctx = new MessageHandler.HandlerContext(client);
        for (MessageHandler<T> handler : handlers) {
            try {
                if (!handler.handle(message, ctx)) {
                    break; // 如果处理器返回false，停止后续处理
                }
            } catch (Exception e) {
                log.error("Error handling message", e);
            }
        }
    }

    /**
     * 添加消息处理器
     * @param handler 要添加的处理器
     */
    public void addHandler(MessageHandler<T> handler) {
        handlers.add(handler);
    }

    /**
     * 移除消息处理器
     * @param handler 要移除的处理器
     */
    public void removeHandler(MessageHandler<T> handler) {
        handlers.remove(handler);
    }
}
