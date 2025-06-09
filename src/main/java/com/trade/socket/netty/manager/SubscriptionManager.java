package com.trade.socket.netty.manager;

import java.util.Set;

/**
 * 订阅管理器接口
 */
public interface SubscriptionManager {

    /**
     * 添加订阅
     * @param topic 订阅主题
     * @param listener 订阅监听器
     */
    void subscribe(String topic, SubscriptionListener listener);

    /**
     * 取消订阅
     * @param topic 订阅主题
     */
    void unsubscribe(String topic);

    /**
     * 获取所有订阅主题
     * @return 订阅主题集合
     */
    Set<String> getSubscriptions();

    /**
     * 恢复所有订阅
     */
    void restoreSubscriptions();

    /**
     * 清除所有订阅
     */
    void clearSubscriptions();

    /**
     * 是否已订阅
     * @param topic 订阅主题
     * @return 订阅状态
     */
    boolean isSubscribed(String topic);

    /**
     * 订阅监听器接口
     */
    interface SubscriptionListener {
        /**
         * 收到订阅消息
         * @param message 消息内容
         */
        void onMessage(String message);

        /**
         * 订阅状态变更
         * @param topic 订阅主题
         * @param subscribed 是否订阅
         */
        void onStatusChange(String topic, boolean subscribed);
    }
}
