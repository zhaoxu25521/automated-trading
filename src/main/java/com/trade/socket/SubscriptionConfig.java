package com.trade.socket;

import java.util.function.Function;

/**
 * 配置类，用于定义订阅和取消订阅消息格式
 */
public class SubscriptionConfig {
    private final Function<String, String> subscribeFormatter;
    private final Function<String, String> unsubscribeFormatter;

    public SubscriptionConfig(Function<String, String> subscribeFormatter,
                              Function<String, String> unsubscribeFormatter) {
        this.subscribeFormatter = subscribeFormatter;
        this.unsubscribeFormatter = unsubscribeFormatter;
    }

    public String formatSubscribe(String subscriptionTopic) {
        return subscribeFormatter.apply(subscriptionTopic);
    }

    public String formatUnsubscribe(String subscriptionTopic) {
        return unsubscribeFormatter.apply(subscriptionTopic);
    }
}
