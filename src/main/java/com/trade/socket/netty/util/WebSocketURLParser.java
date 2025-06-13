package com.trade.socket.netty.util;

import lombok.Getter;

/**
 * WebSocket URL解析工具类
 */
public class WebSocketURLParser {
    private static final String WS_PROTOCOL = "ws://";
    private static final String WSS_PROTOCOL = "wss://";
    private static final int DEFAULT_WS_PORT = 80;
    private static final int DEFAULT_WSS_PORT = 443;

    /**
     * 解析WebSocket URL
     * @param url WebSocket URL (ws://host:port 或 wss://host:port)
     * @return 解析后的URL信息
     * @throws IllegalArgumentException 当URL格式无效时抛出
     */
    public static WebSocketURL parse(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        boolean ssl = false;
        String remaining;

        // 检查协议
        if (url.startsWith(WSS_PROTOCOL)) {
            ssl = true;
            remaining = url.substring(WSS_PROTOCOL.length());
        } else if (url.startsWith(WS_PROTOCOL)) {
            remaining = url.substring(WS_PROTOCOL.length());
        } else {
            throw new IllegalArgumentException("Invalid WebSocket protocol. Must start with ws:// or wss://");
        }

        // 分离主机和端口
        String host;
        int port;
        int colonIndex = remaining.indexOf(':');
        int slashIndex = remaining.indexOf('/');

        if (colonIndex > 0 && (slashIndex == -1 || colonIndex < slashIndex)) {
            // 包含端口号
            host = remaining.substring(0, colonIndex);
            try {
                port = Integer.parseInt(remaining.substring(colonIndex + 1,
                    slashIndex > 0 ? slashIndex : remaining.length()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number", e);
            }
        } else {
            // 不包含端口号，使用默认端口
            host = slashIndex > 0 ? remaining.substring(0, slashIndex) : remaining;
            port = ssl ? DEFAULT_WSS_PORT : DEFAULT_WS_PORT;
        }

        // 验证主机
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Host cannot be empty");
        }

        return new WebSocketURL(url,host, port, ssl);
    }

    /**
     * WebSocket URL信息类
     */
    public static class WebSocketURL {
        @Getter
        private final String url;
        private final String host;
        private final int port;
        private final boolean ssl;

        public WebSocketURL(String url,String host, int port, boolean ssl) {
            this.url = url;
            this.host = host;
            this.port = port;
            this.ssl = ssl;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public boolean isSsl() {
            return ssl;
        }

        @Override
        public String toString() {
            return (ssl ? "wss://" : "ws://") + host + ":" + port;
        }
    }
}
