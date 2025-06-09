package com.trade.socket.netty.manager;

import com.trade.socket.netty.client.NettyClient;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认连接管理器
 */
public class DefaultConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionManager.class);

    private final String host;
    private final int port;
    private final NettyClient<?> client;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final int maxRetries;
    private final long retryInterval;

    public DefaultConnectionManager(String host, int port, NettyClient<?> client) {
        this(host, port, client, 3, 5000);
    }

    public DefaultConnectionManager(String host, int port, NettyClient<?> client,
                                  int maxRetries, long retryInterval) {
        this.host = host;
        this.port = port;
        this.client = client;
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    /**
     * 初始化连接
     */
    public void initConnection() {
        if (connecting.compareAndSet(false, true)) {
            new Thread(this::doConnectWithRetry).start();
        }
    }

    private void doConnectWithRetry() {
        int retryCount = 0;
        while (retryCount < maxRetries && !client.isConnected()) {
            try {
                ChannelFuture future = client.connect();
                future.sync();
                logger.info("Connected to {}:{} successfully", host, port);
                connecting.set(false);
                return;
            } catch (Exception e) {
                retryCount++;
                logger.warn("Connection to {}:{} failed (attempt {}/{}), retrying in {}ms...",
                          host, port, retryCount, maxRetries, retryInterval, e);
                try {
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        connecting.set(false);
    }

    /**
     * 关闭连接
     */
    public void shutdown() {
        client.disconnect();
    }
}
