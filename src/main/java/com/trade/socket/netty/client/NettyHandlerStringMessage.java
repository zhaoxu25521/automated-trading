package com.trade.socket.netty.client;

import com.trade.socket.netty.handler.MessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyHandlerStringMessage extends SimpleChannelInboundHandler<String> {
    private final MessageDispatcher<String> messageDispatcher;
    private ChannelPromise handshakeFuture;
    private WebSocketClientHandshaker handshaker;

    public NettyHandlerStringMessage(MessageDispatcher<String> messageDispatcher, WebSocketClientHandshaker handshaker) {
        this.messageDispatcher = messageDispatcher;
        this.handshaker = handshaker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg)  {
            log.info("msg : {} ", msg);
//        if (!handshaker.isHandshakeComplete()) {
//            try {
//                handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
//                handshakeFuture.setSuccess();
//            } catch (WebSocketHandshakeException e) {
//                handshakeFuture.setFailure(e);
//            }
//            return;
//        }
//        if (msg instanceof PongWebSocketFrame) {
//            System.out.println("收到服务端" + ctx.channel().remoteAddress() + "发来的心跳：PONG");
//        }
//
//        if (msg instanceof TextWebSocketFrame) {
//            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
//            messageDispatcher.dispatch(frame.text());
////            System.out.println("收到服务端" + ctx.channel().remoteAddress() + "发来的消息：" + frame.text()); // 接收服务端发送过来的消息
//        }
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("客户端下线");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error",cause);
        ctx.close();
    }
}
