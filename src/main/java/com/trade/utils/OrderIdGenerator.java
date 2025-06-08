package com.trade.utils;

import java.math.BigDecimal;

/**
 * 订单 ID 生成器，生成确定性的客户端订单 ID (clOrdId)。
 */
public class OrderIdGenerator {
    private final String prefix; // 策略 ID 作为前缀
    private final int scale;

    public OrderIdGenerator(String strategyId,int scale) {
        this.prefix = strategyId + "-";
        this.scale = scale;
    }

    /**
     * 为开仓单生成 clOrdId，格式: {strategyId}-open-{side}-{price}
     */
    public String generateOpenOrderId(String side, BigDecimal price) {
        String formattedPrice = price.setScale(scale, BigDecimal.ROUND_DOWN).toString();
        return prefix + "open-" + side + "-" + formattedPrice;
    }

    /**
     * 为平仓单生成 clOrdId，格式: {strategyId}-close-{linkedClOrdId}
     */
    public String generateCloseOrderId(String linkedClOrdId) {
        return prefix + "close-" + linkedClOrdId;
    }
}