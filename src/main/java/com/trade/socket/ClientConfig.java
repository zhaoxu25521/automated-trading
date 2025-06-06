package com.trade.socket;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.Arrays;

// 客户端配置类，包含URL和SSL上下文
public class ClientConfig {
    final URI uri;
    final String host;
    final int port;
    final boolean isSecure;
    final SslContext sslContext;

    ClientConfig(String wsUrl, boolean trustAllCertificates, String[] supportedProtocols, String[] supportedCiphers) throws SSLException {
        this.uri = URI.create(wsUrl);
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
    }
}
