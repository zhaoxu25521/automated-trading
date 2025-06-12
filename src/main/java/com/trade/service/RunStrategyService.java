package com.trade.service;

import com.trade.domain.Exchange;
import com.trade.domain.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RunStrategyService {
    @Autowired
    private TradeService tradeService;
    @Autowired
    private ExchangeService exchangeService;
    public void start(Long id){

        Strategy strategy = tradeService.getById(id);
        Exchange exchange = exchangeService.getById(strategy.getExchangeId());
        log.info("Starting strategy {}", exchange);

    }
}
