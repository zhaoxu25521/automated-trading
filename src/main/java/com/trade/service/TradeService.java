package com.trade.service;

import com.trade.domain.Strategy;
import com.trade.mapper.TradeMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeService {
    private final TradeMapper tradeMapper;

    public TradeService(TradeMapper tradeMapper) {
        this.tradeMapper = tradeMapper;
    }

    @Cacheable(value = "trades", key = "#id")
    public Strategy getById(Long id) {
        return tradeMapper.selectById(id);
    }

    public List<Strategy> getAll() {
        return tradeMapper.selectList(null);
    }

    public int save(Strategy trade) {
        return tradeMapper.insert(trade);
    }

    @CacheEvict(value = "trades", key = "#trade.id")
    public int update(Strategy trade) {
        return tradeMapper.updateById(trade);
    }

    @CacheEvict(value = "trades", key = "#id")
    public int delete(Long id) {
        return tradeMapper.deleteById(id);
    }
}
