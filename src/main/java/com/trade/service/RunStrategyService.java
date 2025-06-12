package com.trade.service;

import com.trade.domain.Strategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RunStrategyService {
    @Autowired
    private TradeService tradeService;
    public void start(Long id){

        Strategy byId = tradeService.getById(id);
        log.info("Starting strategy {}", byId);

    }
}
