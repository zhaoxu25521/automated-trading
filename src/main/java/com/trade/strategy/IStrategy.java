package com.trade.strategy;

import com.trade.domain.Strategy;

/**
 * 策略接口
 */
public interface IStrategy {
    /**
     * 初始化策略
     * @param strategy 策略配置
     */
    void init(Strategy strategy);

    /**
     * 启动策略
     */
    void start();

    /**
     * 停止策略
     */
    void stop();

    /**
     * 更新策略参数
     * @param params 新的策略参数
     */
    void updateParams(String params);

    /**
     * 获取策略状态
     * @return 策略状态
     */
    String getStatus();
}
