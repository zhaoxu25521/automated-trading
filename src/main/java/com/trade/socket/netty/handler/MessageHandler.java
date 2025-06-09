package com.trade.socket.netty.handler;

/**
 * 消息处理器接口
 * @param <T> 消息类型
 */
public interface MessageHandler<T> {

    /**
     * 处理消息
     * @param message 消息内容
     */
    void handle(T message);

    /**
     * 处理异常
     * @param cause 异常原因
     */
    void handleException(Throwable cause);

    /**
     * 是否支持该消息类型
     * @param message 消息内容
     * @return 是否支持
     */
    boolean supports(T message);

    /**
     * 设置下一个处理器
     * @param nextHandler 下一个处理器
     */
    void setNextHandler(MessageHandler<T> nextHandler);
}
