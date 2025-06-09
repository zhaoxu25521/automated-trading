package com.trade.socket.netty.test;

import com.trade.socket.netty.client.NettyClient;
import com.trade.socket.netty.client.NettyClientFactory;
import com.trade.socket.netty.manager.DefaultConnectionManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Netty客户端性能测试类
 */
public class PerformanceTest {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);
    private static final String TEST_SERVER = "ws://echo.websocket.org";
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final int MESSAGE_SIZE_BYTES = 1024; // 1KB

    @Test
    public void testThroughput() throws Exception {
        // 创建测试消息
        String testMessage = buildTestMessage(MESSAGE_SIZE_BYTES);

        // 创建客户端
        NettyClient<String> client = NettyClientFactory.createDefaultClient(TEST_SERVER);
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
                "echo.websocket.org", 80, client);
        connectionManager.initConnection();

        // 等待连接建立
        waitForConnection(client);

        // 预热
        logger.info("Starting warmup...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            client.send(testMessage);
        }
        logger.info("Warmup completed");

        // 性能测试
        logger.info("Starting performance test...");
        CountDownLatch latch = new CountDownLatch(TEST_ITERATIONS);
        AtomicLong totalTime = new AtomicLong(0);

        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long sendTime = System.nanoTime();
            client.send(testMessage);
            totalTime.addAndGet(System.nanoTime() - sendTime);
            latch.countDown();
        }
        long endTime = System.nanoTime();

        // 计算指标
        long totalDurationNs = endTime - startTime;
        double throughput = (double) TEST_ITERATIONS / (totalDurationNs / 1_000_000_000.0);
        double avgLatency = totalTime.get() / (double) TEST_ITERATIONS / 1_000_000.0; // in ms

        logger.info("Performance results:");
        logger.info("  Messages sent: {}", TEST_ITERATIONS);
        logger.info("  Message size: {} bytes", MESSAGE_SIZE_BYTES);
        logger.info("  Total duration: {} ms", totalDurationNs / 1_000_000);
        logger.info("  Throughput: {:.2f} msg/sec", throughput);
        logger.info("  Average latency: {:.2f} ms", avgLatency);

        // 关闭连接
        connectionManager.shutdown();
    }

    private String buildTestMessage(int sizeBytes) {
        StringBuilder sb = new StringBuilder(sizeBytes);
        sb.append("PERF_TEST");
        while (sb.length() < sizeBytes) {
            sb.append("X");
        }
        return sb.substring(0, sizeBytes);
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
