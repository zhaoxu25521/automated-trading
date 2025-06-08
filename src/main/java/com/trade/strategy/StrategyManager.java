package com.trade.strategy;

import com.alibaba.fastjson.JSONObject;
import com.trade.dto.ExchangeConfig;
import com.trade.dto.StrategyConfig;
import com.trade.exchange.ExchangeFactory;
import com.trade.socket.MessageData;
import com.trade.socket.NettySocketClient;
import com.trade.socket.subscript.OkxSubScript;
import com.trade.strategy.impl.ContractHedgingGridStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.trade.socket.constants.ExchangeConstant.*;

@Slf4j
@Component
public class StrategyManager {

    final ExchangeFactory exchangeFactory;
    NettySocketClient nettySocketClient = null;

    private final Map<String, TradeStrategy> strategies;
    private final Map<String, Set<String>> strategySubscriptions;

    public StrategyManager(ExchangeFactory exchangeFactory) {
        this.exchangeFactory = exchangeFactory;
        this.strategies = new ConcurrentHashMap<>();
        this.strategySubscriptions = new ConcurrentHashMap<>();
        nettySocketClient = new NettySocketClient(this::distributeMessage);
    }

    public void connectSocket(ExchangeConfig exchangeConfig){
        try {
            nettySocketClient.startClient(exchangeConfig,
                    OKX_SUBSCRIPTION_FORMAT,
                    OKX_CALL_BACK,
                    false,
                    null,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addStrategy(StrategyConfig config) {
        try {
            boolean clientActive = nettySocketClient.isClientActive(config.getExchange());
            if(!clientActive){
                return;
            }
            TradeStrategy strategy = new ContractHedgingGridStrategy(exchangeFactory.get(config.getSymbol()),config);
            strategies.put(strategy.getKey(), strategy);
            strategySubscriptions.putIfAbsent(strategy.getKey(), ConcurrentHashMap.newKeySet());
            config.getSubscribedTopics().forEach(v->{
                nettySocketClient.subscribe(config.getExchange(), v);
            });
            System.out.println("策略 " + strategy.getKey() + " 已添加，订阅主题: " + config.getSubscribedTopics());
//            System.out.println("策略 " + strategy.getKey() + " 已添加，订阅主题: ");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startStrategy(String strategyId, String clientId) {
        TradeStrategy strategy = strategies.get(strategyId);
        if (strategy != null) {
            try {
                strategy.start();
                strategySubscriptions.get(strategyId).addAll(strategy.getConfig().getSubscribedTopics());
//                strategy.getConfig().getSubscribedTopics().forEach(topic -> socketClient.subscribe(clientId, topic));
                System.out.println("策略 " + strategyId + " 已启动，订阅主题: " + strategy.getConfig().getSubscribedTopics());
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

    public void distributeMessage(MessageData message) {
        log.info("message: {}", JSONObject.toJSONString(message));
        // 按优先级排序分发消息
        List<TradeStrategy> sortedStrategies = strategies.values().stream()
                .filter(s -> s.getStatus() == TradeStrategy.Status.RUNNING)
                .sorted(Comparator.comparingInt(s -> s.getConfig().getPriority()))
                .collect(Collectors.toList());
        for (TradeStrategy strategy : sortedStrategies) {
//            strategy.handleMessage(message);
        }
    }

    public Map<String, TradeStrategy.Status> getAllStrategiesStatus() {
        return strategies.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus()));
    }

    public TradeStrategy.Status getStrategyStatus(String strategyId) {
        TradeStrategy strategy = strategies.get(strategyId);
        return strategy != null ? strategy.getStatus() : null;
    }

    // 自动重启策略（在客户端重连后调用）
    public void autoRestartStrategies(String clientId) {
        strategies.values().stream()
                .filter(s -> s.getConfig().getIsAutoRestart() && s.getStatus() != TradeStrategy.Status.RUNNING)
                .forEach(s -> startStrategy(s.getKey(), clientId));
    }
}
