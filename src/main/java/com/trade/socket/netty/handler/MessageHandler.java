package com.trade.socket.netty.handler;

import com.trade.socket.netty.client.NettyClient;

/**
    package com.trade.socket.netty.handler;

/**
 * 消息处理器接口
 * @param <T> 消息类型
 */
public interface MessageHandler<T> {
    /*
    * 处理消息
     * @param message 接收到的消息
     * @param ctx 处理上下文
     * @return 是否继续处理下一个处理器
     */
    boolean handle(T message, HandlerContext ctx);

    /**
     * 处理器链上下文
     */
    class HandlerContext {
        private final NettyClient client;

        public HandlerContext(NettyClient client) {
            this.client = client;
        }

        public NettyClient getClient() {
            return client;
        }
    }
}
