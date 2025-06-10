package com.trade.exception;

/**
 * 策略异常类
 */
public class StrategyException extends RuntimeException {
    public StrategyException() {
        super();
    }

    public StrategyException(String message) {
        super(message);
    }

    public StrategyException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrategyException(Throwable cause) {
        super(cause);
    }
}
