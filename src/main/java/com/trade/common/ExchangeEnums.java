package com.trade.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易所枚举
 */
public class ExchangeEnums {

    @AllArgsConstructor
    public enum SocketExchange{
        okx_public("okx-public","okx-public"),
        okx_private("okx-private","okx-private"),
        okx_simulated_public("okx-public-sumulated","okx-模拟盘-公共"),
        okx_simulated_private("okx-private-sumulated","okx-模拟盘-私有"),
        okx_simulated_business("okx-busioness-sumulated","okx-模拟盘-业务"),
            ;
        @Getter
        private String code;
        @Getter
        private String msg;

    }

    /**
     * 开平仓模式下，side和posSide需要进行组合
     * 开多：买入开多（side 填写 buy； posSide 填写 long ）
     * 开空：卖出开空（side 填写 sell； posSide 填写 short ）
     * 平多：卖出平多（side 填写 sell；posSide 填写 long ）
     * 平空：买入平空（side 填写 buy； posSide 填写 short ）
     */
    @AllArgsConstructor
    public enum OrderSide{
        BUY("buy","买"),
        SELL("sell","卖"),
        ;
        @Getter
        private String code;
        @Getter
        private String msg;
    }

    @AllArgsConstructor
    public enum Direction{
        LONG("long","多"),
        SHORT("short","空"),
        ;
        @Getter
        private String code;
        @Getter
        private String msg;
    }

    @AllArgsConstructor
    public enum OrderType{
        LIMIT("","限价委托"),

        ;
        @Getter
        private String code;
        @Getter
        private String msg;
    }

}
