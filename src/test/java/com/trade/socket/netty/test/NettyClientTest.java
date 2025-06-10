package com.trade.socket.netty.test;

import com.trade.socket.netty.client.NettyClient;
import com.trade.socket.netty.client.NettyClientFactory;
import com.trade.socket.netty.handler.MessageHandler;
import com.trade.socket.netty.manager.DefaultConnectionManager;
import com.trade.socket.netty.manager.DefaultSubscriptionManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Netty客户端测试类
 */
public class NettyClientTest {
    private static final Logger logger = LoggerFactory.getLogger(NettyClientTest.class);
    private static final String TEST_SERVER = "ws://echo.websocket.org";
    private static final String TEST_SSL_SERVER = "wss://echo.websocket.org";
    private static final int TIMEOUT_SECONDS = 10;
    private static final String HEARTBEAT_MESSAGE = "HEARTBEAT";
    private static final int HEARTBEAT_INTERVAL = 1;

    @Test
    public void testWebSocketConnection() throws Exception {
        // 创建客户端
        NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SERVER);

        // 创建订阅管理器
        DefaultSubscriptionManager<String> subscriptionManager = new DefaultSubscriptionManager<>(client);

        // 添加测试消息处理器
        CountDownLatch latch = new CountDownLatch(1);
        subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
            @Override
            public boolean handle(String message, MessageHandler.HandlerContext ctx) {
                logger.info("Received message: {}", message);
                latch.countDown();
                return true;
            }
        });

        // 创建连接管理器并启动连接
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 80, client);
        connectionManager.initConnection();

        // 发送测试消息
        client.send("Test message");

        // 等待消息接收
        assertTrue("Did not receive message within timeout",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // 关闭连接
        connectionManager.shutdown();
    }

    @Test
    public void testSSLWebSocketConnection() throws Exception {
        // 创建SSL客户端
        NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SSL_SERVER);

        // 创建订阅管理器
        DefaultSubscriptionManager<String> subscriptionManager = new DefaultSubscriptionManager<>(client);

        // 添加测试消息处理器
        CountDownLatch latch = new CountDownLatch(1);
        subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
            @Override
            public boolean handle(String message, MessageHandler.HandlerContext ctx) {
                logger.info("Received SSL message: {}", message);
                latch.countDown();
                return true;
            }
        });

        // 创建连接管理器并启动连接
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 443, client);
        connectionManager.initConnection();

        // 发送测试消息
        client.send("Test SSL message");

        // 等待消息接收
        assertTrue("Did not receive SSL message within timeout",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // 关闭连接
        connectionManager.shutdown();
    }

    @Test
    public void testHeartbeat() throws Exception {
        // 创建带心跳的客户端
        NettyClient<String> client = NettyClientFactory.createCustomClient(
                TEST_SERVER, HEARTBEAT_INTERVAL, HEARTBEAT_MESSAGE);

        // 创建订阅管理器
        DefaultSubscriptionManager<String> subscriptionManager = new DefaultSubscriptionManager<>(client);

        // 添加心跳消息处理器
        CountDownLatch latch = new CountDownLatch(3); // 等待3次心跳
        subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
            @Override
            public boolean handle(String message, MessageHandler.HandlerContext ctx) {
                if (HEARTBEAT_MESSAGE.equals(message)) {
                    logger.info("Received heartbeat message");
                    latch.countDown();
                }
                return true;
            }
        });

        // 创建连接管理器并启动连接
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 80, client);
        connectionManager.initConnection();

        // 等待心跳消息
        assertTrue("Did not receive enough heartbeat messages within timeout",
                latch.await(HEARTBEAT_INTERVAL * 5, TimeUnit.SECONDS));

        // 关闭连接
        connectionManager.shutdown();
    }

    @Test
    public void testReconnection() throws Exception {
        // 创建客户端
        NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SERVER);

        // 创建带重试的连接管理器
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 80, client, 5, 1000);
        connectionManager.initConnection();

        // 等待初始连接
        assertTrue("Client did not connect initially",
                waitForConnection(client, TIMEOUT_SECONDS));

        // 模拟断开连接
        client.disconnect();
        assertFalse("Client should be disconnected", client.isConnected());

        // 等待重连
        assertTrue("Client did not reconnect within timeout",
                waitForConnection(client, TIMEOUT_SECONDS));

        // 关闭连接
        connectionManager.shutdown();
    }

    private boolean waitForConnection(NettyClient<?> client, int timeoutSeconds) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < endTime) {
            if (client.isConnected()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        return false;
    }
}
