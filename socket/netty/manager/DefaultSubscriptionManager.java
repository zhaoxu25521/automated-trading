package com.trade.socket.netty.manager;

import com.trade.socket.netty.client.NettyClient;
import com.trade.socket.netty.handler.MessageHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认订阅管理器
 * @param <T> 消息类型
 */
public class DefaultSubscriptionManager<T> {
    private final NettyClient<T> client;
    private final Map<String, List<MessageHandler<T>>> topicSubscriptions = new ConcurrentHashMap<>();
    private final List<MessageHandler<T>> globalHandlers = new CopyOnWriteArrayList<>();

    public DefaultSubscriptionManager(NettyClient<T> client) {
        this.client = client;
    }

    /**
     * 订阅指定主题
     * @param topic 主题名称
     * @param handler 消息处理器
     */
    public void subscribe(String topic, MessageHandler<T> handler) {
        topicSubscriptions.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * 取消订阅指定主题
     * @param topic 主题名称
     * @param handler 要移除的处理器
     */
    public void unsubscribe(String topic, MessageHandler<T> handler) {
        List<MessageHandler<T>> handlers = topicSubscriptions.get(topic);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                topicSubscriptions.remove(topic);
            }
        }
    }

    /**
     * 添加全局处理器（处理所有消息）
     * @param handler 消息处理器
     */
    public void addGlobalHandler(MessageHandler<T> handler) {
        globalHandlers.add(handler);
    }

    /**
     * 移除全局处理器
     * @param handler 要移除的处理器
     */
    public void removeGlobalHandler(MessageHandler<T> handler) {
        globalHandlers.remove(handler);
    }

    /**
     * 处理接收到的消息
     * @param topic 消息主题
     * @param message 消息内容
     */
    public void handleMessage(String topic, T message) {
        // 处理全局处理器
        for (MessageHandler<T> handler : globalHandlers) {
            handler.handle(message, new MessageHandler.HandlerContext(client));
        }

        // 处理主题订阅处理器
        List<MessageHandler<T>> handlers = topicSubscriptions.get(topic);
        if (handlers != null) {
            for (MessageHandler<T> handler : handlers) {
                handler.handle(message, new MessageHandler.HandlerContext(client));
            }
        }
    }

    /**
     * 获取所有订阅的主题
     * @return 主题集合
     */
    public Set<String> getSubscribedTopics() {
        return Collections.unmodifiableSet(topicSubscriptions.keySet());
    }
}
