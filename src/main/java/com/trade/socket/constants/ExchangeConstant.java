package com.trade.socket.constants;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trade.socket.ClientConfig;
import com.trade.socket.SubscriptionCallback;
import com.trade.socket.SubscriptionConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeConstant {

    // 定义OKX客户端客户端的订阅格式
    public static final SubscriptionConfig OKX_SUBSCRIPTION_FORMAT = new SubscriptionConfig(
            subscriptionTopic -> {
                JSONObject sub= new JSONObject();
                sub.put("id",System.currentTimeMillis());
                sub.put("op","subscribe");
                JSONArray jsonArray = new JSONArray();
                jsonArray.add(JSONObject.parseObject(subscriptionTopic));
                sub.put("args",jsonArray);
                return sub.toString();
            },
            subscriptionTopic -> {
                JSONObject sub= new JSONObject();
                sub.put("op","unsubscribe");
                JSONArray jsonArray = new JSONArray();
                jsonArray.add(JSONObject.parseObject(subscriptionTopic));
                sub.put("args",jsonArray);
                return sub.toString();
            }
    );

    // 定义订阅回调
    public static final SubscriptionCallback OKX_CALL_BACK = new SubscriptionCallback() {
        @Override
        public void onSuccess(String clientId, String subscriptionTopic, String action) {
            System.out.println("Callback: " + clientId + " " + action + " to " + subscriptionTopic + " succeeded");
        }

        @Override
        public void onFailure(String clientId, String subscriptionTopic, String action, String errorMessage) {
            System.err.println("Callback: " + clientId + " " + action + " to " + subscriptionTopic + " failed: " + errorMessage);
        }
    };

    // Socket 管理
    public static final Map<String, ClientConfig> CLIENT_CONFIG = new ConcurrentHashMap<>();
    public static final Map<String, Bootstrap> clientBootstraps = new ConcurrentHashMap<>();
    public static final Map<String, Channel> clientChannels = new ConcurrentHashMap<>();
    public static final Map<String, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    public static final Map<String, SubscriptionConfig> clientSubscriptionConfigs = new ConcurrentHashMap<>();
    public static final Map<String, SubscriptionCallback> clientSubscriptionCallbacks = new ConcurrentHashMap<>();
    public static final Map<String, WebSocketClientHandshaker> clientHandshakers = new ConcurrentHashMap<>();
    public static final Map<String, Integer> reconnectAttempts = new ConcurrentHashMap<>();


    /**** params ****/
    // 是否测试环境
    public static final String isTest = "isTest";
    // socket 地址
    public static final String WS_URL = "wsUrl";
    // socket test地址
    public static final String WS_TEST_URL = "wsTestUrl";
    // rest 地址
    public static final String REST_URL = "restUrl";
    // ping
    public static final String PING = "ping";
    //
    public static final String PING_INTERVAL_TIME = "IntervalMs";
    //
    public static final String PING_TIMEOUT_TIME = "TimeoutMs";
}
