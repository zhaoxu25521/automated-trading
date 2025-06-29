package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.MessageDispatcher;
import com.trade.socket.netty.util.WebSocketURLParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Netty基础客户端实现
 */
@Slf4j
public class BaseNettyClient<T> implements NettyClient {
    private final int heartbeatInterval;
    private final String heartbeatMessage;
    private final MessageDispatcher<String> messageDispatcher;
    private final WebSocketURLParser.WebSocketURL socketUrl;
    private EventLoopGroup workerGroup;
    private Channel channel;
    private volatile boolean running;
    private WebSocketClientHandshaker handshaker;

    public BaseNettyClient(WebSocketURLParser.WebSocketURL socketUrl, int heartbeatInterval, String heartbeatMessage, MessageDispatcher<String> messageDispatcher) {
        this.socketUrl = socketUrl;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatMessage = heartbeatMessage;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public ChannelFuture connect() {
        workerGroup = new NioEventLoopGroup();
        running = true;

        try {
            // 配置SSL上下文（如果需要）
            SslContext sslContext;
            if (socketUrl.isSsl()) {
                sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            } else {
                sslContext = null;
            }

            // 创建WebSocket握手器
            handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                new URI(socketUrl.getUrl()), WebSocketVersion.V13, null, true, new DefaultHttpHeaders());

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            if (sslContext != null) {
                                ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), socketUrl.getHost(), socketUrl.getPort()));
                            }
                            // 添加HTTP编解码器
                            ch.pipeline().addLast(new HttpClientCodec());
                            // 添加HTTP消息聚合器
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));

//                            ch.pipeline().addLast(new WebSocketClientProtocolHandler(handshaker));

                            ch.pipeline().addLast(new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS));
//                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
//                                @Override
//                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//                                    log.info("-----------------{}",evt);
//                                    if (evt instanceof IdleStateEvent) {
//                                        IdleStateEvent event = (IdleStateEvent) evt;
//                                        if (event.state() == IdleState.WRITER_IDLE) {
//                                            ctx.writeAndFlush(new TextWebSocketFrame("ping"));
//                                            System.out.println("----------Sent ping");
//                                        }
//                                    }
//                                    super.userEventTriggered(ctx, evt);
//                                }
//                            });
//                            // 添加WebSocket协议处理器
//                            ch.pipeline().addLast(new io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler(handshaker));
//                            // 添加自定义消息处理器
                            ch.pipeline().addLast(new NettyHandlerStringMessage(messageDispatcher,handshaker));
                            ch.pipeline().addLast(new NettyHandlerObjectMessage(messageDispatcher,handshaker));
                        }
                    });

            ChannelFuture future = bootstrap.connect(socketUrl.getHost(), socketUrl.getPort()).sync();
            channel = future.channel();

            // 等待握手完成
            handshaker.handshake(channel).sync();

            // 启动心跳
            startHeartbeat();

            return future;
        } catch (Exception e) {
            log.error("Failed to connect", e);
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
    public void send(String message) {
        log.info("2222222222 {} , {} , {}",message,channel != null,channel.isActive());
        if (channel != null && channel.isActive()) {
            log.info("0000000000000000");

//            channel.writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
            channel.writeAndFlush(new TextWebSocketFrame(message));
        }
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    private void startHeartbeat() {
        workerGroup.scheduleAtFixedRate(() -> {
            if (running && isConnected()) {
                send(heartbeatMessage);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
    }
}
