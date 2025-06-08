package com.trade.socket;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.trade.socket.constants.ExchangeConstant.WS_URL;

// 客户端配置类，包含URL和SSL上下文
public class ClientConfig {
    final URI uri;
    final String host;
    final int port;
    final boolean isSecure;
    final SslContext sslContext;
    final Map<String,String> params;
    final AtomicBoolean lastHeartbeatResponded; // 跟踪上次心跳是否收到响应

    ClientConfig(Map<String,String> params, boolean trustAllCertificates, String[] supportedProtocols, String[] supportedCiphers) throws SSLException {
        this.params = params;
        this.uri = URI.create(params.get(WS_URL));
        String scheme = uri.getScheme();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Invalid WebSocket URL scheme: " + scheme);
        }
        this.host = uri.getHost();
        this.port = uri.getPort() == -1 ? ("wss".equalsIgnoreCase(scheme) ? 443 : 80) : uri.getPort();
        this.isSecure = "wss".equalsIgnoreCase(scheme);
        if (isSecure) {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            if (trustAllCertificates) {
                sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }
            if (supportedProtocols != null && supportedProtocols.length > 0) {
                sslContextBuilder.protocols(supportedProtocols);
            }
            if (supportedCiphers != null && supportedCiphers.length > 0) {
                sslContextBuilder.ciphers(Arrays.asList(supportedCiphers));
            }
            this.sslContext = sslContextBuilder.build();
        } else {
            this.sslContext = null;
        }
        this.lastHeartbeatResponded = new AtomicBoolean(true);
    }
}
