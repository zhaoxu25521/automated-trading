package com.trade.strategy;

import com.trade.dto.StrategyConfig;
import com.trade.exchange.ExchangeFactory;
import com.trade.strategy.impl.ContractHedgingGridStrategy;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class StrategyManager {

    final ExchangeFactory exchangeFactory;

    private final Map<String, TradeStrategy> strategies;
    private final Map<String, Set<String>> strategySubscriptions;

    public StrategyManager(ExchangeFactory exchangeFactory) {
        this.exchangeFactory = exchangeFactory;
        this.strategies = new ConcurrentHashMap<>();
        this.strategySubscriptions = new ConcurrentHashMap<>();
    }

    public void addStrategy(StrategyConfig config) {
        TradeStrategy strategy = new ContractHedgingGridStrategy(exchangeFactory.get(config.getSymbol()),config);
        strategies.put(strategy.getKey(), strategy);
        strategySubscriptions.putIfAbsent(strategy.getKey(), ConcurrentHashMap.newKeySet());
//        System.out.println("策略 " + strategy.getKey() + " 已添加，订阅主题: " + config.getSubscribedTopics());
        System.out.println("策略 " + strategy.getKey() + " 已添加，订阅主题: ");
    }

    public void startStrategy(String strategyId, String clientId) {
        TradeStrategy strategy = strategies.get(strategyId);
        if (strategy != null) {
            try {
                strategy.start();
                strategySubscriptions.get(strategyId).addAll(strategy.getConfig().getSubscribedTopics());
                strategy.getConfig().getSubscribedTopics().forEach(topic -> socketClient.subscribe(clientId, topic));
                System.out.println("策略 " + strategyId + " 已启动，订阅主题: " + strategy.getSubscribedTopics());
                if (strategy.getConfig().getIsAutoRestart()) {
                    System.out.println("策略 " + strategyId + " 已启用自动重启");
                }
            } catch (Exception e) {
                System.err.println("启动策略 " + strategyId + " 失败: " + e.getMessage());
                strategy.setStatus(TradeStrategy.Status.ERROR);
            }
        } else {
            System.err.println("策略 " + strategyId + " 不存在");
        }
    }

    public void stopStrategy(String strategyId, String clientId) {
        TradeStrategy strategy = strategies.get(strategyId);
        if (strategy != null) {
            try {
                strategy.stop();
//                strategySubscriptions.get(strategyId).forEach(topic -> socketClient.unsubscribe(clientId, topic));
                strategySubscriptions.get(strategyId).clear();
                System.out.println("策略 " + strategyId + " 已停止");
            } catch (Exception e) {
                System.err.println("停止策略 " + strategyId + " 失败: " + e.getMessage());
                strategy.setStatus(TradeStrategy.Status.ERROR);
            }
        } else {
            System.err.println("策略 " + strategyId + " 不存在");
        }
    }

    public void removeStrategy(String strategyId) {
        TradeStrategy strategy = strategies.remove(strategyId);
        if (strategy != null) {
            strategySubscriptions.remove(strategyId);
            System.out.println("策略 " + strategyId + " 已移除");
        }
    }

    public void distributeMessage(String message) {
        // 按优先级排序分发消息
        List<TradeStrategy> sortedStrategies = strategies.values().stream()
                .filter(s -> s.getStatus() == TradeStrategy.Status.RUNNING)
                .sorted(Comparator.comparingInt(s -> s.getConfig().getPriority()))
                .collect(Collectors.toList());
        for (WebProperties.Resources.Chain.Strategy strategy : sortedStrategies) {
            strategy.handleMessage(message);
        }
    }

    public Map<String, TradeStrategy.Status> getAllStrategiesStatus() {
        return strategies.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus()));
    }

    public Strategy.Status getStrategyStatus(String strategyId) {
        Strategy strategy = strategies.get(strategyId);
        return strategy != null ? strategy.getStatus() : null;
    }

    // 自动重启策略（在客户端重连后调用）
    public void autoRestartStrategies(String clientId) {
        strategies.values().stream()
                .filter(s -> s.getConfig().isAutoRestart() && s.getStatus() != Strategy.Status.RUNNING)
                .forEach(s -> startStrategy(s.getId(), clientId));
    }
}
