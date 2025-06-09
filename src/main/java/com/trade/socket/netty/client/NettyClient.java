package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.MessageHandler;
import io.netty.channel.ChannelFuture;

/**
 * Netty客户端基础接口
 * @param <T> 消息类型
 */
public interface NettyClient<T> {

    /**
     * 连接服务器
     * @param host 主机地址
     * @param port 端口号
     * @return ChannelFuture
     */
    ChannelFuture connect(String host, int port);

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 是否连接
     * @return 连接状态
     */
    boolean isConnected();

    /**
     * 发送消息
     * @param message 消息内容
     */
    void send(T message);

    /**
     * 添加消息处理器
     * @param handler 消息处理器
     */
    void addMessageHandler(MessageHandler<T> handler);

    /**
     * 移除消息处理器
     * @param handler 消息处理器
     */
    void removeMessageHandler(MessageHandler<T> handler);
}
