package com.trade.collection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trade.socket.NettySocketClient;
import com.trade.socket.SubscriptionCallback;
import com.trade.socket.SubscriptionConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Consumer;

@RestController
@RequestMapping("/api")
public class TestCollection {
    static String OKX_WS_URL = "wss://wspap.okx.com:8443/ws/v5/public";
    // 定义订阅回调
    SubscriptionCallback okxCallback = new SubscriptionCallback() {
        @Override
        public void onSuccess(String clientId, String subscriptionTopic, String action) {
            System.out.println("Callback: " + clientId + " " + action + " to " + subscriptionTopic + " succeeded");
        }

        @Override
        public void onFailure(String clientId, String subscriptionTopic, String action, String errorMessage) {
            System.err.println("Callback: " + clientId + " " + action + " to " + subscriptionTopic + " failed: " + errorMessage);
        }
    };
    // 定义不同客户端的订阅格式
    SubscriptionConfig jsonConfig = new SubscriptionConfig(
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


    NettySocketClient client;
    @GetMapping("sub")
    public void sub(){
        // 启动多个客户端实例，分别使用不同订阅格式和回调
        JSONObject object = new JSONObject();
        object.put("channel","tickers");
        object.put("instId","BTC-USDT");
        // 订阅主题
        client.subscribe("client1", object.toString());

    }
    @GetMapping("/connect")
    public void connect() throws Exception {
        // 创建消息处理器
        Consumer<String> messageHandler = System.out::println;
        // 初始化客户端
        client = new NettySocketClient(messageHandler);

        client.startClient("client1", OKX_WS_URL, jsonConfig, okxCallback,false,new String[]{},new String[]{});
    }



}
