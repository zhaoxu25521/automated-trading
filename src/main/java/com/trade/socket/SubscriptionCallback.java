package com.trade.socket;

/**
 * 订阅状态回调接口
 */
public interface SubscriptionCallback {
    void onSuccess(String clientId, String subscriptionTopic, String action);
    void onFailure(String clientId, String subscriptionTopic, String action, String errorMessage);
}
