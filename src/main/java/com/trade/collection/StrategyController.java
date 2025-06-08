package com.trade.collection;

import com.alibaba.fastjson.JSONObject;
import com.trade.dto.ExchangeConfig;
import com.trade.dto.StrategyConfig;
import com.trade.enums.ExchangeEnums;
import com.trade.strategy.StrategyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.trade.constants.ExchangeConstant.*;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    @Autowired
    private StrategyManager strategyManager;

    @GetMapping("/connect")
    public void connect(){

        ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setExchangeName(ExchangeEnums.OKX.name());
        Map<String,String> params = new HashMap<>();
        params.put(WS_URL,"wss://wspap.okx.com:8443/ws/v5/public");
//        params.put(PING,"{\"op\":\"ping\"}");
        params.put(PING,"ping");
        params.put(PING_INTERVAL_TIME,"30000");
        params.put(PING_TIMEOUT_TIME,"60000");

        exchangeConfig.setParms(params);
        strategyManager.connectSocket(exchangeConfig);
    }
    @GetMapping("/add")
    public void add(){
        HashSet<String> objects = new HashSet<>();
        JSONObject object = new JSONObject();
        object.put("channel","tickers");
        object.put("instId","BTC-USDT");
        objects.add(object.toString());
        StrategyConfig config = new // 初始化配置
                 StrategyConfig(
                ExchangeEnums.OKX.name(),
                new BigDecimal("1.5"), // hedgeRatio
                new BigDecimal("0.01"), // gridInterval
                "BTC-USDT", // symbol
                3, // gridCount
                8, // priceScale
                8, // quantityScale
                1000, // monitorIntervalMs
                500, // retryDelayMs
                10, // leverage
                new BigDecimal("10.0"), // maxPosition
                new BigDecimal("1000.0"), // minMargin
                new BigDecimal("1.0"), // baseQuantity
                new BigDecimal("0.01"), // riskPerTrade
                new BigDecimal("5000.0"), // stopLoss
                new BigDecimal("10000.0"), // takeProfit
                14, // atrPeriod
                new BigDecimal("1.5"), // atrMultiplier
                new HashSet<String>() {{
                    add(object.toString());
                }} // subscribedTopics
        );;
        strategyManager.addStrategy(config);
    }

    @GetMapping("/start")
    public void startStrategy(@RequestParam("key")String key){
        strategyManager.startStrategy(key);
    }
}
