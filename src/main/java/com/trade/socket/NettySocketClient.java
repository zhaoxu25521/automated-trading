package com.trade.socket;

import com.trade.dto.ExchangeConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.trade.socket.constants.ExchangeConstant.*;

public class NettySocketClient {
    private final EventLoopGroup group;
    private final Consumer<MessageData> messageHandler;
    private volatile boolean running;
    private final int maxReconnectAttempts = 5;
    private final long reconnectDelayMs = 3000;

    public NettySocketClient(Consumer<MessageData> messageHandler) {
        this.messageHandler = message -> {
            messageHandler.accept(message);
            // 处理心跳响应
            if (message.getMessage().contains("\"op\":\"pong\"")) {
                ClientConfig config = CLIENT_CONFIG.get(message.getExchange());
                if (config != null) {
                    config.lastHeartbeatResponded.set(true);
                    System.out.println("客户端 " + message.getExchange() + " 收到心跳响应: " + message);
                }
            }
        };
//        this.messageHandler = messageHandler;
        this.group = new NioEventLoopGroup();
        this.running = false;
    }
    private String getClientIdByMessage(String message) {
        // 简化处理，实际可根据消息内容或上下文映射
        return clientChannels.keySet().stream().findFirst().orElse(null);
    }
    private Bootstrap createBootstrap(ClientConfig config) {
        Bootstrap bootstrap = new Bootstrap();
        Map<String, String> params = config.params;
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (config.isSecure) {
                            pipeline.addLast(config.sslContext.newHandler(ch.alloc(), config.host, config.port));
                        }
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(8192));
                        // 增强心跳机制
                        pipeline.addLast(new IdleStateHandler(
                                Long.valueOf(params.get(PING_INTERVAL_TIME)) / 1000, // 读空闲超时
                                Long.valueOf(params.get(PING_TIMEOUT_TIME)) / 1000, // 写空闲超时
                                0, TimeUnit.SECONDS));
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                String clientId = getClientId(ctx.channel().id().asLongText());
                                if (clientId != null) {
                                    ClientConfig clientConfig = CLIENT_CONFIG.get(clientId);
                                    if (clientConfig != null) {
                                        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                                                clientConfig.uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
                                        clientHandshakers.put(clientId, handshaker);
                                        handshaker.handshake(ctx.channel());
                                    }
                                }
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                String clientId = getClientId(ctx.channel().id().asLongText());
                                if (clientId == null) {
                                    ctx.close();
                                    return;
                                }
                                WebSocketClientHandshaker handshaker = clientHandshakers.get(clientId);
                                if (!handshaker.isHandshakeComplete()) {
                                    try {
                                        handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                                        System.out.println("客户端 " + clientId + " WebSocket 握手完成");
                                        ctx.pipeline().addLast(new SimpleChannelInboundHandler<WebSocketFrame>() {
                                            @Override
                                            protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
                                                if (frame instanceof TextWebSocketFrame) {
                                                    String message = ((TextWebSocketFrame) frame).text();
                                                    messageHandler.accept(new MessageData(clientId, message));
                                                    if (message.startsWith("ACK:")) {
                                                        handleAckMessage(clientId, message);
                                                    }
                                                } else if (frame instanceof BinaryWebSocketFrame) {
                                                    System.err.println("客户端 " + clientId + " 收到二进制帧，未处理");
                                                }
                                            }
                                        });
                                        resubscribe(clientId); // 握手完成后重新订阅
                                    } catch (WebSocketHandshakeException e) {
                                        System.err.println("客户端 " + clientId + " WebSocket 握手失败: " + e.getMessage());
                                        ctx.close();
                                    }
                                    return;
                                }
                                ctx.fireChannelRead(msg);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) {
                                String clientId = getClientId(ctx.channel().id().asLongText());
                                clientChannels.remove(clientId);
                                clientHandshakers.remove(clientId);
                                if (running && clientId != null) {
                                    scheduleReconnect(clientId);
                                }
                            }

                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                if (evt instanceof IdleStateEvent) {
                                    IdleStateEvent event = (IdleStateEvent) evt;
                                    String clientId = getClientId(ctx.channel().id().asLongText());
                                    if (clientId == null) return;
                                    ClientConfig config = CLIENT_CONFIG.get(clientId);
                                    if (config == null) return;

                                    if (event.state() == IdleState.WRITER_IDLE) {
                                        // 发送心跳
                                        if (!config.lastHeartbeatResponded.get()) {
                                            System.err.println("客户端 " + clientId + " 上次心跳未收到响应，关闭连接");
                                            ctx.close();
                                            return;
                                        }
                                        ctx.writeAndFlush(new TextWebSocketFrame(params.get(PING)))
                                                .addListener(future -> {
                                                    if (future.isSuccess()) {
                                                        System.out.println("客户端 " + clientId + " 发送心跳: " + params.get(PING));
                                                        config.lastHeartbeatResponded.set(false);
                                                    } else {
                                                        System.err.println("客户端 " + clientId + " 发送心跳失败: " + future.cause().getMessage());
                                                        ctx.close();
                                                    }
                                                });
                                    } else if (event.state() == IdleState.READER_IDLE) {
                                        // 读空闲超时，关闭连接
                                        System.err.println("客户端 " + clientId + " 读空闲超时，未收到服务器数据，关闭连接");
                                        ctx.close();
                                    }
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                System.err.println("客户端通道错误: " + cause.getMessage());
                                cause.printStackTrace();
                                ctx.close();
                            }
                        });
                    }
                });
        return bootstrap;
    }

    public void startClient(ExchangeConfig exchangeConfig, SubscriptionConfig config, SubscriptionCallback callback,
                            boolean trustAllCertificates, String[] supportedProtocols, String[] supportedCiphers) throws Exception {
        if (!running) {
            running = true;
        }
        ClientConfig clientConfig = new ClientConfig(exchangeConfig.getParms(), trustAllCertificates, supportedProtocols, supportedCiphers);
        CLIENT_CONFIG.put(exchangeConfig.getExchangeName(), clientConfig);
        clientBootstraps.put(exchangeConfig.getExchangeName(), createBootstrap(clientConfig));
        clientSubscriptions.putIfAbsent(exchangeConfig.getExchangeName(), ConcurrentHashMap.newKeySet());
        clientSubscriptionConfigs.put(exchangeConfig.getExchangeName(), config != null ? config :
                new SubscriptionConfig(
                        subscriptionTopic -> "{\"op\":\"subscribe\",\"args\":[" + subscriptionTopic + "]}",
                        subscriptionTopic -> "{\"op\":\"unsubscribe\",\"args\":[" + subscriptionTopic + "]}"
                ));
        clientSubscriptionCallbacks.put(exchangeConfig.getExchangeName(), callback);
        reconnectAttempts.put(exchangeConfig.getExchangeName(), 0);
        connect(exchangeConfig.getExchangeName());
    }

    private void connect(String clientId) throws InterruptedException {
        ClientConfig config = CLIENT_CONFIG.get(clientId);
        Bootstrap bootstrap = clientBootstraps.get(clientId);
        if (config == null || bootstrap == null) {
            System.err.println("客户端 " + clientId + " 无配置或引导程序");
            return;
        }
        ChannelFuture future = bootstrap.connect(config.host, config.port).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                clientChannels.put(clientId, f.channel());
                reconnectAttempts.put(clientId, 0);
                System.out.println("客户端 " + clientId + " 已连接到 " + config.host + ":" + config.port);
            } else {
                System.err.println("客户端 " + clientId + " 连接失败: " + f.cause().getMessage());
                scheduleReconnect(clientId);
            }
        });
        try {
            future.sync();
        } catch (Exception e) {
            System.err.println("客户端 " + clientId + " 连接错误: " + e.getMessage());
            scheduleReconnect(clientId);
        }
    }

    private void scheduleReconnect(String clientId) {
        if (clientId == null) return;
        int attempts = reconnectAttempts.getOrDefault(clientId, 0);
        if (attempts < maxReconnectAttempts && running) {
            reconnectAttempts.put(clientId, attempts + 1);
            System.out.println("客户端 " + clientId + " 重连尝试 " + (attempts + 1) + "/" + maxReconnectAttempts);
            group.schedule(() -> {
                try {
                    connect(clientId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, reconnectDelayMs, TimeUnit.MILLISECONDS);
        } else {
            System.err.println("客户端 " + clientId + " 已达最大重连次数或已停止");
            reconnectAttempts.remove(clientId);
            clientSubscriptions.remove(clientId);
            clientSubscriptionConfigs.remove(clientId);
            clientSubscriptionCallbacks.remove(clientId);
            CLIENT_CONFIG.remove(clientId);
            clientBootstraps.remove(clientId);
            clientHandshakers.remove(clientId);
        }
    }

    public void subscribe(String clientId, String subscriptionTopic) {
        Channel channel = clientChannels.get(clientId);
        SubscriptionConfig config = clientSubscriptionConfigs.get(clientId);
        SubscriptionCallback callback = clientSubscriptionCallbacks.get(clientId);
        if (channel != null && channel.isActive() && config != null) {
            String subscribeMsg = config.formatSubscribe(subscriptionTopic);
            channel.writeAndFlush(new TextWebSocketFrame(subscribeMsg)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    clientSubscriptions.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(subscriptionTopic);
                    System.out.println("客户端 " + clientId + " 订阅 " + subscriptionTopic + "，消息: " + subscribeMsg);
                } else if (callback != null) {
                    callback.onFailure(clientId, subscriptionTopic, "subscribe", "发送订阅消息失败: " + future.cause().getMessage());
                }
            });
        } else {
            String error = channel == null || !channel.isActive() ? "客户端未连接" : "无订阅配置";
            System.err.println("客户端 " + clientId + " 订阅失败: " + error);
            if (callback != null) {
                callback.onFailure(clientId, subscriptionTopic, "subscribe", error);
            }
        }
    }

    public void unsubscribe(String clientId, String subscriptionTopic) {
        Channel channel = clientChannels.get(clientId);
        SubscriptionConfig config = clientSubscriptionConfigs.get(clientId);
        SubscriptionCallback callback = clientSubscriptionCallbacks.get(clientId);
        if (channel != null && channel.isActive() && config != null) {
            String unsubscribeMsg = config.formatUnsubscribe(subscriptionTopic);
            channel.writeAndFlush(new TextWebSocketFrame(unsubscribeMsg)).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Set<String> topics = clientSubscriptions.get(clientId);
                    if (topics != null) {
                        topics.remove(subscriptionTopic);
                        System.out.println("客户端 " + clientId + " 取消订阅 " + subscriptionTopic + "，消息: " + unsubscribeMsg);
                    }
                } else if (callback != null) {
                    callback.onFailure(clientId, subscriptionTopic, "unsubscribe", "发送取消订阅消息失败: " + future.cause().getMessage());
                }
            });
        } else {
            String error = channel == null || !channel.isActive() ? "客户端未连接" : "无订阅配置";
            System.err.println("客户端 " + clientId + " 取消订阅失败: " + error);
            if (callback != null) {
                callback.onFailure(clientId, subscriptionTopic, "unsubscribe", error);
            }
        }
    }

    private void resubscribe(String clientId) {
        Set<String> topics = clientSubscriptions.get(clientId);
        SubscriptionConfig config = clientSubscriptionConfigs.get(clientId);
        SubscriptionCallback callback = clientSubscriptionCallbacks.get(clientId);
        if (topics != null && !topics.isEmpty() && config != null) {
            Channel channel = clientChannels.get(clientId);
            if (channel != null && channel.isActive()) {
                for (String subscriptionTopic : topics) {
                    String subscribeMsg = config.formatSubscribe(subscriptionTopic);
                    channel.writeAndFlush(new TextWebSocketFrame(subscribeMsg)).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            System.out.println("客户端 " + clientId + " 重新订阅 " + subscriptionTopic + "，消息: " + subscribeMsg);
                        } else if (callback != null) {
                            callback.onFailure(clientId, subscriptionTopic, "resubscribe", "发送重新订阅消息失败: " + future.cause().getMessage());
                        }
                    });
                }
            }
        }
    }

    private void handleAckMessage(String clientId, String message) {
        SubscriptionCallback callback = clientSubscriptionCallbacks.get(clientId);
        if (callback == null) return;
        String[] parts = message.split(":", 3);
        if (parts.length == 3 && parts[0].equals("ACK")) {
            String action = parts[1];
            String subscriptionTopic = parts[2];
            if (action.equals("SUBSCRIBE") || action.equals("UNSUBSCRIBE")) {
                callback.onSuccess(clientId, subscriptionTopic, action);
            }
        }
    }

    private String getClientId(String channelId) {
        return clientChannels.entrySet().stream()
                .filter(entry -> entry.getValue().id().asLongText().equals(channelId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void sendMessage(String clientId, String message) {
        Channel channel = clientChannels.get(clientId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(message));
        } else {
            System.err.println("客户端 " + clientId + " 未连接");
        }
    }

    public void stopClient(String clientId) {
        Channel channel = clientChannels.remove(clientId);
        reconnectAttempts.remove(clientId);
        clientSubscriptions.remove(clientId);
        clientSubscriptionConfigs.remove(clientId);
        clientSubscriptionCallbacks.remove(clientId);
        CLIENT_CONFIG.remove(clientId);
        clientBootstraps.remove(clientId);
        clientHandshakers.remove(clientId);
        if (channel != null) {
            channel.close();
            System.out.println("客户端 " + clientId + " 已停止");
        }
    }

    public void shutdown() {
        running = false;
        clientChannels.values().forEach(Channel::close);
        clientChannels.clear();
        reconnectAttempts.clear();
        clientSubscriptions.clear();
        clientSubscriptionConfigs.clear();
        clientSubscriptionCallbacks.clear();
        CLIENT_CONFIG.clear();
        clientBootstraps.clear();
        clientHandshakers.clear();
        group.shutdownGracefully();
        System.out.println("Netty客户端已关闭");
    }

    public boolean isClientActive(String clientId) {
        Channel channel = clientChannels.get(clientId);
        return channel != null && channel.isActive();
    }

    public int getActiveClientsCount() {
        return clientChannels.size();
    }
}
