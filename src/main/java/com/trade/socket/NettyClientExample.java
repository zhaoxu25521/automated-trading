package com.trade.socket;

import com.trade.enums.ExchangeEnums;
import org.json.JSONObject;

import java.util.function.Consumer;

import static com.trade.socket.constants.ExchangeConstant.OKX_SUBSCRIPTION_FORMAT;

public class NettyClientExample {
    static String OKX_WS_URL = "wss://wspap.okx.com:8443/ws/v5/public";
    public static void main(String[] args) throws Exception {

        // 创建消息处理器
        Consumer<MessageData> messageHandler = System.out::println;

        // 初始化客户端


//        SubscriptionConfig simpleConfig = new SubscriptionConfig(
//                subscriptionTopic -> "SUB:" + subscriptionTopic,
//                subscriptionTopic -> "UNSUB:" + subscriptionTopic
//        );

        // 启动多个客户端实例，分别使用不同订阅格式和回调
        JSONObject object = new JSONObject();
        object.put("channel","tickers");
        object.put("instId","BTC-USDT");

        // 初始化客户端
        NettySocketClient client = new NettySocketClient(messageHandler);

        // 定义订阅回调
        SubscriptionCallback callback = new SubscriptionCallback() {
            @Override
            public void onSuccess(String clientId, String subscriptionTopic, String action) {
                System.out.println("Callback: " + clientId + " " + action + " to " + subscriptionTopic + " succeeded");
            }

            @Override
            public void onFailure(String clientId, String subscriptionTopic, String action, String errorMessage) {
                System.err.println("Callback: " + clientId + " " + action + " to " + subscriptionTopic + " failed: " + errorMessage);
            }
        };
//        SubscriptionConfig simpleConfig = new SubscriptionConfig(
//                subscriptionTopic -> "SUB:" + subscriptionTopic,
//                subscriptionTopic -> "UNSUB:" + subscriptionTopic
//        );

        // 启动多个客户端实例，分别使用不同的WebSocket URL和订阅格式
//        client.startClient(ExchangeEnums.OKX.name(), "ws://localhost:8080/websocket1", OKX_SUBSCRIPTION_FORMAT, callback,false,new String[]{},new String[]{});
//        client.startClient("client2", "wss://example.com:443/websocket2", simpleConfig, callback);

        // 订阅主题
        client.subscribe(ExchangeEnums.OKX.name(), "topic1");
//        client.subscribe("client1", "topic2");
//        client.subscribe("client2", "topic1");

        // 发送消息
//        client.sendMessage("client1", "Hello from client1");
//        client.sendMessage("client2", "Hello from client2");

        // 等待片刻以观察消息处理
        Thread.sleep(2000);

        // 取消订阅
        client.unsubscribe("client1", "topic2");

        // 检查客户端状态
        System.out.println("Active clients: " + client.getActiveClientsCount());
        System.out.println("Is client1 active? " + client.isClientActive("client1"));

        // 模拟断开后重连会自动重新订阅
        client.stopClient("client1");
        Thread.sleep(1000);
//        client.startClient("client1", "ws://localhost:8080/websocket1", jsonConfig, callback);

        // 等待片刻观察重连和重新订阅
        Thread.sleep(4000);

        // 关闭所有客户端
        client.shutdown();
    }
}
