package com.trade.socket.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

/**
 * Netty基础客户端实现
 */
public class BaseNettyClient<T> implements NettyClient<T> {
    private final String host;
    private final int port;
    private final int heartbeatInterval;
    private final String heartbeatMessage;
    private final boolean sslEnabled;

    private EventLoopGroup workerGroup;
    private Channel channel;
    private volatile boolean running;

    public BaseNettyClient(String host, int port, int heartbeatInterval, String heartbeatMessage) {
        this(host, port, heartbeatInterval, heartbeatMessage, false);
    }

    public BaseNettyClient(String host, int port, int heartbeatInterval, String heartbeatMessage, boolean sslEnabled) {
        this.host = host;
        this.port = port;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatMessage = heartbeatMessage;
        this.sslEnabled = sslEnabled;
    }

    @Override
    public ChannelFuture connect() {
        workerGroup = new NioEventLoopGroup();
        running = true;

        try {
            // 配置SSL上下文（如果需要）
            SslContext sslContext = null;
            if (sslEnabled) {
                sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            }

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            if (sslContext != null) {
                                ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, port));
                            }
                            // 添加其他处理器
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();

            // 启动心跳
            startHeartbeat();

            return future;
        } catch (InterruptedException | SSLException e) {
            throw new RuntimeException("Failed to connect", e);
        }
    }

    @Override
    public void disconnect() {
        running = false;
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void send(T message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
        }
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    private void startHeartbeat() {
        workerGroup.scheduleAtFixedRate(() -> {
            if (running && isConnected()) {
                send((T) heartbeatMessage);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
    }
}
