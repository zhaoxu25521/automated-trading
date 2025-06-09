package com.trade.socket.netty.manager;

import com.trade.socket.netty.client.NettyClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认连接管理器实现
 */
public class DefaultConnectionManager implements ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionManager.class);

    private final String host;
    private final int port;
    private final NettyClient<?> nettyClient;

    private volatile ChannelFuture channelFuture;
    private boolean autoReconnect = true;
    private int reconnectAttempts = 0;
    private final int maxReconnectAttempts = 5;
    private final List<ConnectionListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    public DefaultConnectionManager(String host, int port, NettyClient<?> nettyClient) {
        this.host = host;
        this.port = port;
        this.nettyClient = nettyClient;
    }

    @Override
    public void initConnection(String host, int port) {
        connect();
    }

    @Override
    public ChannelFuture getConnection() {
        return channelFuture;
    }

    @Override
    public void closeConnection() {
        autoReconnect = false;
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        reconnectExecutor.shutdown();
    }

    @Override
    public boolean isConnected() {
        return channelFuture != null && channelFuture.channel().isActive();
    }

    @Override
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    private void connect() {
        channelFuture = nettyClient.connect(host, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                onConnected();
            } else {
                onConnectionFailed(future.cause());
            }
        });
    }

    private void onConnected() {
        reconnectAttempts = 0;
        listeners.forEach(ConnectionListener::onConnected);
        channelFuture.channel().closeFuture().addListener(future -> {
            if (autoReconnect) {
                onDisconnected();
            }
        });
    }

    private void onDisconnected() {
        listeners.forEach(ConnectionListener::onDisconnected);
        scheduleReconnect();
    }

    private void onConnectionFailed(Throwable cause) {
        logger.error("Connection failed", cause);
        if (autoReconnect) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            long delay = Math.min(5000, 1000 * (1 << reconnectAttempts));
            reconnectExecutor.schedule(() -> {
                reconnectAttempts++;
                logger.info("Attempting to reconnect (attempt {}/{})", reconnectAttempts, maxReconnectAttempts);
                connect();
                listeners.forEach(ConnectionListener::onReconnect);
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            logger.error("Max reconnect attempts reached");
        }
    }
}
