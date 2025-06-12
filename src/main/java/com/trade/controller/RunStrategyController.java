package com.trade.controller;

import com.trade.result.Result;
import com.trade.service.RunStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/run")
public class RunStrategyController {
    @Autowired
    private RunStrategyService runStrategyService;
    @GetMapping
    public Result runStrategy(@RequestParam("id")Long id) {
        runStrategyService.start(id);
        return Result.success();
    }
}
