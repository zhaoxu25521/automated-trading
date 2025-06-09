package com.trade.socket.netty.manager;

import io.netty.channel.ChannelFuture;

/**
 * 连接管理器接口
 */
public interface ConnectionManager {

    /**
     * 初始化连接
     * @param host 主机地址
     * @param port 端口号
     */
    void initConnection(String host, int port);

    /**
     * 获取连接
     * @return ChannelFuture
     */
    ChannelFuture getConnection();

    /**
     * 关闭连接
     */
    void closeConnection();

    /**
     * 是否连接
     * @return 连接状态
     */
    boolean isConnected();

    /**
     * 设置自动重连
     * @param autoReconnect 是否自动重连
     */
    void setAutoReconnect(boolean autoReconnect);

    /**
     * 添加连接监听器
     * @param listener 监听器
     */
    void addConnectionListener(ConnectionListener listener);

    /**
     * 移除连接监听器
     * @param listener 监听器
     */
    void removeConnectionListener(ConnectionListener listener);

    /**
     * 连接监听器接口
     */
    interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onReconnect();
    }
}
