package com.trade.exchange;

import com.trade.enums.ExchangeEnums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExchangeFactory {

    private Map<String,ExchangeApi> exchangeApiMap = new HashMap<>();

    public ExchangeFactory(){
        exchangeApiMap.put(ExchangeEnums.BINANCE.name(),new BinanceFuturesApi());
        exchangeApiMap.put(ExchangeEnums.OKX.name(), new OkxFuturesApi());
    }

    public ExchangeApi get(String name){
        return exchangeApiMap.get(name);
    }


}
