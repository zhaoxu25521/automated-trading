package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.HeartbeatHandler;
import com.trade.socket.netty.handler.MessageHandler;
import com.trade.socket.netty.manager.ConnectionManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基础Netty客户端实现
 * @param <T> 消息类型
 */
public class BaseNettyClient<T> implements NettyClient<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseNettyClient.class);

    private final String host;
    private final int port;
    private final int heartbeatInterval;
    private final String heartbeatMessage;

    private EventLoopGroup workerGroup;
    private Channel channel;
    private ConnectionManager connectionManager;
    private final List<MessageHandler<T>> messageHandlers = new CopyOnWriteArrayList<>();

    public BaseNettyClient(String host, int port, int heartbeatInterval, String heartbeatMessage) {
        this.host = host;
        this.port = port;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatMessage = heartbeatMessage;
    }

    @Override
    public ChannelFuture connect(String host, int port) {
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HeartbeatHandler(heartbeatInterval, heartbeatMessage));
                            // 添加其他处理器...
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            this.channel = future.channel();
            return future;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Connection interrupted", e);
            return null;
        } catch (Exception e) {
            logger.error("Connection failed", e);
            return null;
        }
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public void send(T message) {
        if (isConnected()) {
            channel.writeAndFlush(message);
        }
    }

    @Override
    public void addMessageHandler(MessageHandler<T> handler) {
        messageHandlers.add(handler);
    }

    @Override
    public void removeMessageHandler(MessageHandler<T> handler) {
        messageHandlers.remove(handler);
    }

    // 其他实现方法...
}
