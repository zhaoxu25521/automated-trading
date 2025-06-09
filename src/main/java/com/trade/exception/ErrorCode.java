package com.trade.exception;

public enum ErrorCode {
    // 系统错误
    SYSTEM_ERROR(1000, "系统错误"),
    SERVICE_UNAVAILABLE(1001, "服务不可用"),

    // 业务错误
    STRATEGY_NOT_FOUND(2000, "策略不存在"),
    STRATEGY_ALREADY_EXISTS(2001, "策略已存在"),
    INVALID_TRADE_PARAM(2002, "交易参数无效"),

    // 参数校验错误
    PARAM_VALIDATION_ERROR(3000, "参数校验失败"),
    MISSING_REQUIRED_PARAM(3001, "缺少必要参数");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
