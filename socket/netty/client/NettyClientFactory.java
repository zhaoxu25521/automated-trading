package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.MessageDispatcher;
import com.trade.socket.netty.manager.DefaultConnectionManager;
import com.trade.socket.netty.manager.DefaultSubscriptionManager;
import com.trade.socket.netty.util.WebSocketURLParser;
import com.trade.socket.netty.util.WebSocketURLParser.WebSocketURL;

import java.util.ArrayList;
import java.util.List;

/**
 * Netty客户端工厂类
 */
public class NettyClientFactory {
    
    /**
     * 创建默认配置的Netty客户端
     * @param url WebSocket URL (ws://host:port 或 wss://host:port)
     * @return NettyClient实例
     * @throws Exception 当URL解析失败时抛出
     */
    public static NettyClient<String> createDefaultClient(String url) throws Exception {
        WebSocketURL wsUrl = WebSocketURLParser.parse(url);
        
        // 默认配置
        int heartbeatInterval = 30; // 30秒心跳间隔
        String heartbeatMessage = "PING"; // 心跳消息内容
        
        // 创建基础客户端
        BaseNettyClient<String> client = new BaseNettyClient<>(wsUrl.getHost(), wsUrl.getPort(),
            heartbeatInterval, heartbeatMessage, wsUrl.isSsl());

        // 创建消息处理器列表
        List<MessageHandler<String>> handlers = new ArrayList<>();

        // 创建消息分发器并添加到处理器链
        MessageDispatcher<String> dispatcher = new MessageDispatcher<>(handlers);

        // 创建连接管理器
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(wsUrl.getHost(),
            wsUrl.getPort(), client);

        // 创建订阅管理器
        DefaultSubscriptionManager subscriptionManager = new DefaultSubscriptionManager(client);

        // 初始化连接
        connectionManager.initConnection(wsUrl.getHost(), wsUrl.getPort());

        return client;
    }

    /**
     * 创建默认配置的Netty客户端 (兼容旧版)
     * @param host 主机地址
     * @param port 端口号
     * @return NettyClient实例
     */
    public static NettyClient<String> createDefaultClient(String host, int port) {
        try {
            String url = "ws://" + host + ":" + port;
            return createDefaultClient(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client", e);
        }
    }

    /**
     * 创建自定义配置的Netty客户端
     * @param url WebSocket URL (ws://host:port 或 wss://host:port)
     * @param heartbeatInterval 心跳间隔(秒)
     * @param heartbeatMessage 心跳消息内容
     * @return NettyClient实例
     * @throws Exception 当URL解析失败时抛出
     */
    public static NettyClient<String> createCustomClient(String url,
                                                       int heartbeatInterval, String heartbeatMessage) throws Exception {
        WebSocketURL wsUrl = WebSocketURLParser.parse(url);
        BaseNettyClient<String> client = new BaseNettyClient<>(wsUrl.getHost(), wsUrl.getPort(),
            heartbeatInterval, heartbeatMessage, wsUrl.isSsl());
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(wsUrl.getHost(),
            wsUrl.getPort(), client);
        connectionManager.initConnection(wsUrl.getHost(), wsUrl.getPort());
        return client;
    }

    /**
     * 创建自定义配置的Netty客户端 (兼容旧版)
     * @param host 主机地址
     * @param port 端口号
     * @param heartbeatInterval 心跳间隔(秒)
     * @param heartbeatMessage 心跳消息内容
     * @return NettyClient实例
     */
    public static NettyClient<String> createCustomClient(String host, int port,
                                                       int heartbeatInterval, String heartbeatMessage) {
        try {
            String url = "ws://" + host + ":" + port;
            return createCustomClient(url, heartbeatInterval, heartbeatMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client", e);
        }
    }

    /**
     * 创建带有自定义处理器的Netty客户端
     * @param url WebSocket URL (ws://host:port 或 wss://host:port)
     * @param handlers 自定义消息处理器列表
     * @return NettyClient实例
     * @throws Exception 当URL解析失败时抛出
     */
    public static NettyClient<String> createClientWithHandlers(String url,
                                                            List<MessageHandler<String>> handlers) throws Exception {
        WebSocketURL wsUrl = WebSocketURLParser.parse(url);
        BaseNettyClient<String> client = new BaseNettyClient<>(wsUrl.getHost(), wsUrl.getPort(),
            30, "PING", wsUrl.isSsl());
        MessageDispatcher<String> dispatcher = new MessageDispatcher<>(handlers);
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(wsUrl.getHost(),
            wsUrl.getPort(), client);
        connectionManager.initConnection(wsUrl.getHost(), wsUrl.getPort());
        return client;
    }

    /**
     * 创建带有自定义处理器的Netty客户端 (兼容旧版)
     * @param host 主机地址
     * @param port 端口号
     * @param handlers 自定义消息处理器列表
     * @return NettyClient实例
     */
    public static NettyClient<String> createClientWithHandlers(String host, int port,
                                                            List<MessageHandler<String>> handlers) {
        try {
            String url = "ws://" + host + ":" + port;
            return createClientWithHandlers(url, handlers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client", e);
        }
    }
}
