package com.trade.socket.netty.manager;

import com.trade.socket.netty.client.NettyClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认订阅管理器实现
 */
public class DefaultSubscriptionManager implements SubscriptionManager {
    private final Map<String, List<SubscriptionListener>> topicListeners = new ConcurrentHashMap<>();
    private final Set<String> persistentSubscriptions = ConcurrentHashMap.newKeySet();
    private final NettyClient<String> nettyClient;

    public DefaultSubscriptionManager(NettyClient<String> nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    public void subscribe(String topic, SubscriptionListener listener) {
        topicListeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
        persistentSubscriptions.add(topic);

        // 发送订阅命令
        nettyClient.send("SUB:" + topic);

        // 通知订阅状态变化
        notifyStatusChange(topic, true, listener);
    }

    @Override
    public void unsubscribe(String topic) {
        List<SubscriptionListener> listeners = topicListeners.remove(topic);
        persistentSubscriptions.remove(topic);

        // 发送取消订阅命令
        nettyClient.send("UNSUB:" + topic);

        // 通知订阅状态变化
        if (listeners != null) {
            listeners.forEach(listener -> notifyStatusChange(topic, false, listener));
        }
    }

    @Override
    public Set<String> getSubscriptions() {
        return Collections.unmodifiableSet(topicListeners.keySet());
    }

    @Override
    public void restoreSubscriptions() {
        persistentSubscriptions.forEach(topic -> {
            nettyClient.send("SUB:" + topic);
            List<SubscriptionListener> listeners = topicListeners.get(topic);
            if (listeners != null) {
                listeners.forEach(listener -> notifyStatusChange(topic, true, listener));
            }
        });
    }

    @Override
    public void clearSubscriptions() {
        topicListeners.keySet().forEach(this::unsubscribe);
        persistentSubscriptions.clear();
    }

    @Override
    public boolean isSubscribed(String topic) {
        return topicListeners.containsKey(topic);
    }

    public void notifyMessage(String topic, String message) {
        List<SubscriptionListener> listeners = topicListeners.get(topic);
        if (listeners != null) {
            listeners.forEach(listener -> listener.onMessage(message));
        }
    }

    private void notifyStatusChange(String topic, boolean subscribed, SubscriptionListener listener) {
        if (listener != null) {
            listener.onStatusChange(topic, subscribed);
        }
    }
}
