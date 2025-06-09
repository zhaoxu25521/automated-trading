package com.trade.socket.netty.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Netty客户端测试套件
 *
 * 执行顺序：
 * 1. 单元测试
 * 2. 性能测试
 * 3. 稳定性测试
 * 4. 集成测试
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    NettyClientTest.class,
    PerformanceTest.class,
    StabilityTest.class,
    IntegrationTest.class
})
public class AllTests {
    // 测试套件容器类，不需要实现方法
    // 使用JUnit的Suite运行器执行所有测试类
}
