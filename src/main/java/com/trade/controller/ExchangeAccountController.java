package com.trade.controller;

import com.trade.domain.ExchangeAccount;
import com.trade.result.Result;
import com.trade.service.ExchangeAccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account")
public class ExchangeAccountController {
    private final ExchangeAccountService accountService;

    public ExchangeAccountController(ExchangeAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public Result<ExchangeAccount> getById(@PathVariable Long id) {
        return Result.success(accountService.getById(id));
    }

    @GetMapping
    public Result<List<ExchangeAccount>> getAll() {
        return Result.success(accountService.getAll());
    }

    @PostMapping
    public Result<Integer> create(@RequestBody ExchangeAccount req) {
        return Result.success(accountService.save(req));
    }

    @PutMapping("/{id}")
    public Result<Integer> update(@PathVariable Long id, @RequestBody ExchangeAccount req) {
        req.setId(id);
        return Result.success(accountService.update(req));
    }

    @DeleteMapping("/{id}")
    public Result<Integer> delete(@PathVariable Long id) {
        return Result.success(accountService.delete(id));
    }
}
