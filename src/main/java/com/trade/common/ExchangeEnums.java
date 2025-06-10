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

}
