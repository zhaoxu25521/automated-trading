package com.trade.strategy;

import com.trade.domain.Strategy;
import com.trade.exception.StrategyException;
import com.trade.strategy.impl.GridStrategy;
import com.trade.strategy.impl.TrendStrategy;

/**
 * 策略工厂类
 */
public class StrategyFactory {
    /**
     * 创建策略实例
     * @param strategy 策略配置
     * @return 策略实例
     * @throws StrategyException 如果策略类型不支持
     */
    public static IStrategy createStrategy(Strategy strategy) throws StrategyException {
        String strategyType = strategy.getName().toLowerCase();

        switch (strategyType) {
            case "grid":
                return new GridStrategy();
            case "trend":
                return new TrendStrategy();
            default:
                throw new StrategyException("Unsupported strategy type: " + strategyType);
        }
    }
}
