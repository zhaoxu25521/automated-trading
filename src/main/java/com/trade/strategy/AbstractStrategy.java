package com.trade.strategy;

import com.trade.common.DataMessage;
import com.trade.domain.Strategy;
import lombok.extern.slf4j.Slf4j;

/**
 * 策略抽象基类
 */
@Slf4j
public abstract class AbstractStrategy implements IStrategy {
    protected Strategy strategy;
    protected boolean running = false;

    @Override
    public void init(Strategy strategy) {
        this.strategy = strategy;
        log.info("Strategy {} initialized", strategy.getName());
    }

    @Override
    public void start() {
        if (running) {
            log.warn("Strategy {} is already running", strategy.getName());
            return;
        }
        running = true;
        log.info("Strategy {} started", strategy.getName());
        onStart();
    }

    @Override
    public void stop() {
        if (!running) {
            log.warn("Strategy {} is not running", strategy.getName());
            return;
        }
        running = false;
        log.info("Strategy {} stopped", strategy.getName());
        onStop();
    }

    @Override
    public void updateParams(String params) {
        strategy.setParams(params);
        log.info("Strategy {} params updated", strategy.getName());
        onParamsUpdate();
    }

    @Override
    public String getStatus() {
        return running ? "RUNNING" : "STOPPED";
    }

    /**
     * 策略启动时的具体逻辑，由子类实现
     */
    protected abstract void onStart();

    /**
     * 策略停止时的具体逻辑，由子类实现
     */
    protected abstract void onStop();

    /**
     * 参数更新时的具体逻辑，由子类实现
     */
    protected abstract void onParamsUpdate();

    public abstract void change(DataMessage dataMessage);
}
