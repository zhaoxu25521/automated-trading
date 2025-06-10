package com.trade.controller;

import com.trade.domain.Exchange;
import com.trade.req.ExchangeReq;
import com.trade.result.Result;
import com.trade.service.ExchangeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {
    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    @GetMapping("/{id}")
    public Result<Exchange> getById(@PathVariable Long id) {
        return Result.success(exchangeService.getById(id));
    }

    @GetMapping
    public Result<List<Exchange>> getAll() {
        return Result.success(exchangeService.getAll());
    }

    @PostMapping
    public Result<Integer> create(@RequestBody Exchange exchange) {
        return Result.success(exchangeService.save(exchange));
    }

    @PutMapping("/{id}")
    public Result<Integer> update(@PathVariable Long id, @RequestBody Exchange req) {
        req.setId(id);
        return Result.success(exchangeService.update(req));
    }

    @DeleteMapping("/{id}")
    public Result<Integer> delete(@PathVariable Long id) {
        return Result.success(exchangeService.delete(id));
    }
}
