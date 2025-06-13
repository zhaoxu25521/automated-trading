package com.trade.socket.netty.handler.impl;

import com.trade.socket.netty.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OkxHandler implements MessageHandler<String> {
    @Override
    public boolean handle(String message, HandlerContext ctx) {
        log.info("Received SSL message: {}", message);
        return true;
    }
}
