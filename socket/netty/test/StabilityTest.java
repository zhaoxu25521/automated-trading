package com.trade.socket.netty.test;

import com.trade.socket.netty.client.NettyClient;
import com.trade.socket.netty.client.NettyClientFactory;
import com.trade.socket.netty.manager.DefaultConnectionManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty客户端稳定性测试类
 */
public class StabilityTest {
    private static final Logger logger = LoggerFactory.getLogger(StabilityTest.class);
    private static final String TEST_SERVER = "ws://echo.websocket.org";
    private static final int TEST_DURATION_MINUTES = 5;
    private static final int MESSAGE_INTERVAL_MS = 100;
    private static final int MEMORY_CHECK_INTERVAL_MS = 5000;

    @Test
    public void testLongRunning() throws Exception {
        // 创建客户端
        NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SERVER);
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 80, client, 5, 1000);
        connectionManager.initConnection();

        // 等待连接建立
        waitForConnection(client);

        // 创建测试控制
        CountDownLatch testLatch = new CountDownLatch(1);
        AtomicInteger messageCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + TimeUnit.MINUTES.toMillis(TEST_DURATION_MINUTES);

        // 启动消息发送线程
        Thread senderThread = new Thread(() -> {
            try {
                while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
                    client.send("Stability test message #" + messageCount.incrementAndGet());
                    TimeUnit.MILLISECONDS.sleep(MESSAGE_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                testLatch.countDown();
            }
        });

        // 启动内存监控线程
        Thread memoryMonitorThread = new Thread(() -> {
            try {
                while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
                    logMemoryUsage();
                    TimeUnit.MILLISECONDS.sleep(MEMORY_CHECK_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 启动测试
        senderThread.start();
        memoryMonitorThread.start();

        // 等待测试完成
        testLatch.await();

        // 清理
        senderThread.interrupt();
        memoryMonitorThread.interrupt();
        connectionManager.shutdown();

        // 输出测试结果
        long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime);
        logger.info("Stability test completed:");
        logger.info("  Duration: {} minutes", durationMinutes);
        logger.info("  Messages sent: {}", messageCount.get());
        logger.info("  Final memory usage:");
        logMemoryUsage();
    }

    private void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        logger.info("Memory usage: Used={}MB, Free={}MB, Total={}MB, Max={}MB",
                usedMemory, freeMemory, totalMemory, runtime.maxMemory() / (1024 * 1024));
    }

    private void waitForConnection(NettyClient<?> client) throws InterruptedException {
        long endTime = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < endTime) {
            if (client.isConnected()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        throw new IllegalStateException("Connection not established within timeout");
    }
}
