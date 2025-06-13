package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.MessageDispatcher;
import com.trade.socket.netty.handler.MessageHandler;
import com.trade.socket.netty.manager.DefaultConnectionManager;
import com.trade.socket.netty.manager.DefaultSubscriptionManager;
import com.trade.socket.netty.util.WebSocketURLParser;
import com.trade.socket.netty.util.WebSocketURLParser.WebSocketURL;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Netty客户端工厂类
 */
@Slf4j
public class NettyClientFactory {
    
    /**
     * 创建默认配置的Netty客户端
     * @param url WebSocket URL (ws://host:port 或 wss://host:port)
     * @return NettyClient实例
     * @throws Exception 当URL解析失败时抛出
     */
    public static NettyClient createDefaultClient(String url) {
        WebSocketURL wsUrl = WebSocketURLParser.parse(url);
        
        // 默认配置
        int heartbeatInterval = 30; // 30秒心跳间隔
        String heartbeatMessage = "ping"; // 心跳消息内容
        
        // 创建消息处理器列表
        List<MessageHandler<String>> handlers = new ArrayList<>();
        
        // 创建消息分发器
        MessageDispatcher<String> dispatcher = new MessageDispatcher<>(handlers, null);
        
        // 创建基础客户端
        BaseNettyClient client = new BaseNettyClient(wsUrl,
            heartbeatInterval, heartbeatMessage, dispatcher);
            
        // 设置客户端的消息分发器
        dispatcher.setClient(client);
        
        // 创建连接管理器
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
            wsUrl.getHost(), wsUrl.getPort(), client);
            
        // 初始化连接
        connectionManager.initConnection();
        
        return client;
    }

    /**
     * 创建默认配置的Netty客户端 (兼容旧版)
     * @param host 主机地址
     * @param port 端口号
     * @return NettyClient实例
     */
    public static NettyClient createDefaultClient(String host, int port) {
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
    public static NettyClient createCustomClient(String url, int heartbeatInterval,
                                                       String heartbeatMessage) throws Exception {
        WebSocketURL wsUrl = WebSocketURLParser.parse(url);
        
        // 创建消息处理器列表
        List<MessageHandler<String>> handlers = new ArrayList<>();
        
        // 创建消息分发器
        MessageDispatcher<String> dispatcher = new MessageDispatcher<>(handlers, null);
        
        // 创建基础客户端
        BaseNettyClient client = new BaseNettyClient(wsUrl,
            heartbeatInterval, heartbeatMessage,  dispatcher);
            
        // 设置客户端的消息分发器
        dispatcher.setClient(client);
        
        // 创建连接管理器
        DefaultConnectionManager connectionManager = new DefaultConnectionManager(
            wsUrl.getHost(), wsUrl.getPort(), client);
            
        // 初始化连接
        connectionManager.initConnection();
        
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
    public static NettyClient createCustomClient(String host, int port,
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
    public static NettyClient createClientWithHandlers(String url,
                                                            List<MessageHandler<String>> handlers,MessageDispatcher<String> dispatcher) throws Exception {
        WebSocketURL wsUrl = WebSocketURLParser.parse(url);
        BaseNettyClient client = new BaseNettyClient(wsUrl,
            30, "PING",dispatcher);

        handlers.forEach(v->dispatcher.addHandler(v));

        DefaultConnectionManager connectionManager = new DefaultConnectionManager(wsUrl.getHost(),
            wsUrl.getPort(), client);
        connectionManager.initConnection();
        return client;
    }

    /**
     * 创建带有自定义处理器的Netty客户端 (兼容旧版)
     * @param host 主机地址
     * @param port 端口号
     * @param handlers 自定义消息处理器列表
     * @return NettyClient实例
     */
    public static NettyClient createClientWithHandlers(String host, int port,
                                                            List<MessageHandler<String>> handlers,MessageDispatcher dispatcher) {
        try {
            String url = "ws://" + host + ":" + port;
            return createClientWithHandlers(url, handlers,dispatcher);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client", e);
        }
    }
}
