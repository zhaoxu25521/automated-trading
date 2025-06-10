package com.trade.service;

import com.trade.domain.Exchange;
import com.trade.mapper.ExchangeMapper;
import com.trade.req.ExchangeReq;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeService {
    private final ExchangeMapper exchangeMapper;

    public ExchangeService(ExchangeMapper exchangeMapper) {
        this.exchangeMapper = exchangeMapper;
    }

    @Cacheable(value = "exchange", key = "#id")
    public Exchange getById(Long id) {
        return exchangeMapper.selectById(id);
    }

    public List<Exchange> getAll() {
        return exchangeMapper.selectList(null);
    }

    public int save(Exchange exchange) {
        return exchangeMapper.insert(exchange);
    }

    @CacheEvict(value = "exchange", key = "#exchange.id")
    public int update(Exchange exchange) {
        return exchangeMapper.updateById(exchange);
    }

    @CacheEvict(value = "exchange", key = "#id")
    public int delete(Long id) {
        return exchangeMapper.deleteById(id);
    }
}
