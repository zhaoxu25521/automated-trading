package com.trade.controller;

import com.trade.domain.Strategy;
import com.trade.result.Result;
import com.trade.service.TradeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class StrategyController {
    private final TradeService tradeService;

    public StrategyController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping("/{id}")
    public Result<Strategy> getById(@PathVariable Long id) {
        return Result.success(tradeService.getById(id));
    }

    @GetMapping
    public Result<List<Strategy>> getAll() {
        return Result.success(tradeService.getAll());
    }

    @PostMapping
    public Result<Integer> create(@RequestBody Strategy trade) {
        return Result.success(tradeService.save(trade));
    }

    @PutMapping("/{id}")
    public Result<Integer> update(@PathVariable Long id, @RequestBody Strategy trade) {
        trade.setId(id);
        return Result.success(tradeService.update(trade));
    }

    @DeleteMapping("/{id}")
    public Result<Integer> delete(@PathVariable Long id) {
        return Result.success(tradeService.delete(id));
    }
}
