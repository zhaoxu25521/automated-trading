package com.trade.service;

import com.trade.domain.ExchangeAccount;
import com.trade.mapper.ExchangeAccountMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeAccountService {
    private final ExchangeAccountMapper accountMapper;

    public ExchangeAccountService(ExchangeAccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Cacheable(value = "account", key = "#id")
    public ExchangeAccount getById(Long id) {
        return accountMapper.selectById(id);
    }

    public List<ExchangeAccount> getAll() {
        return accountMapper.selectList(null);
    }

    public int save(ExchangeAccount exchange) {
        return accountMapper.insert(exchange);
    }

    @CacheEvict(value = "account", key = "#account.id")
    public int update(ExchangeAccount exchange) {
        return accountMapper.updateById(exchange);
    }

    @CacheEvict(value = "exchange", key = "#id")
    public int delete(Long id) {
        return accountMapper.deleteById(id);
    }
}
