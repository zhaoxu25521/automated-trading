package com.trade.socket.netty.test;

import com.trade.socket.netty.client.NettyClient;
import com.trade.socket.netty.client.NettyClientFactory;
import com.trade.socket.netty.handler.MessageHandler;
import com.trade.socket.netty.manager.DefaultConnectionManager;
import com.trade.socket.netty.manager.DefaultSubscriptionManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty客户端集成测试类
 */
public class IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
    private static final String TEST_SERVER = "ws://echo.websocket.org";
    private static final int CLIENT_COUNT = 5;
    private static final int TEST_DURATION_SECONDS = 30;
    private static final int MESSAGE_INTERVAL_MS = 500;

    @Test
    public void testMultipleClients() throws Exception {
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(CLIENT_COUNT);
        CountDownLatch testLatch = new CountDownLatch(CLIENT_COUNT);
        AtomicInteger totalMessages = new AtomicInteger(0);
        List<NettyClient<String>> clients = new ArrayList<>();

        // 创建并启动多个客户端
        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int clientId = i + 1;
            executor.submit(() -> {
                try {
                    NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SERVER);
                    clients.add(client);

                    // 创建订阅管理器
                    DefaultSubscriptionManager<String> subscriptionManager = new DefaultSubscriptionManager<>(client);
                    CountDownLatch messageLatch = new CountDownLatch(5);

                    // 添加消息处理器
                    subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
                        @Override
                        public boolean handle(String message, MessageHandler.HandlerContext ctx) {
                            logger.info("Client {} received: {}", clientId, message);
                            messageLatch.countDown();
                            return true;
                        }
                    });

                    // 创建连接管理器
                    DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                            "echo.websocket.org", 80, client, 3, 1000);
                    connectionManager.initConnection();

                    // 发送测试消息
                    long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TEST_DURATION_SECONDS);
                    while (System.currentTimeMillis() < endTime) {
                        String msg = String.format("Message %d from client %d",
                                totalMessages.incrementAndGet(), clientId);
                        client.send(msg);
                        TimeUnit.MILLISECONDS.sleep(MESSAGE_INTERVAL_MS);
                    }

                    // 等待消息接收
                    messageLatch.await(10, TimeUnit.SECONDS);

                    // 关闭连接
                    connectionManager.shutdown();
                } catch (Exception e) {
                    logger.error("Client {} failed", clientId, e);
                } finally {
                    testLatch.countDown();
                }
            });
        }

        // 等待所有客户端完成
        testLatch.await();
        executor.shutdown();

        // 输出测试结果
        logger.info("Integration test completed:");
        logger.info("  Clients: {}", CLIENT_COUNT);
        logger.info("  Duration: {} seconds", TEST_DURATION_SECONDS);
        logger.info("  Total messages sent: {}", totalMessages.get());
    }

    @Test
    public void testErrorRecovery() throws Exception {
        // 创建客户端
        NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SERVER);

        // 创建订阅管理器
        DefaultSubscriptionManager<String> subscriptionManager = new DefaultSubscriptionManager<>(client);
        CountDownLatch messageLatch = new CountDownLatch(3);

        // 添加消息处理器
        subscriptionManager.addGlobalHandler(new MessageHandler<String>() {
            @Override
            public boolean handle(String message, MessageHandler.HandlerContext ctx) {
                logger.info("Received: {}", message);
                messageLatch.countDown();
                return true;
            }
        });

        // 创建连接管理器
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 80, client, 3, 1000);
        connectionManager.initConnection();

        // 发送初始消息
        client.send("Initial message");

        // 模拟网络中断
        logger.info("Simulating network interruption...");
        client.disconnect();
        TimeUnit.SECONDS.sleep(2);

        // 验证自动重连
        logger.info("Waiting for reconnection...");
        client.send("Message after interruption");

        // 等待消息接收
        assertTrue("Did not receive enough messages after reconnection",
                messageLatch.await(10, TimeUnit.SECONDS));

        // 关闭连接
        connectionManager.shutdown();
    }

    private void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
