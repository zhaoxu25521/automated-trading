package com.trade.socket.netty.client;

import io.netty.channel.ChannelFuture;

/**
 * Netty客户端接口
 */
public interface NettyClient {
    /**
     * 连接到服务器
     * @return ChannelFuture
     */
    ChannelFuture connect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 发送消息
     * @param message 要发送的消息
     */
    void send(String message);

    /**
     * 检查是否已连接
     * @return 是否已连接
     */
    boolean isConnected();
}
