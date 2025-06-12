package com.trade.utils;


public class SnowflakeIdGenerator {

    // 起始时间戳 (纪元时间，根据你的服务上线时间调整，越早越好，但不可变)
    // 推荐使用一个比你的服务上线时间更早的时间戳，例如 2020-01-01 00:00:00 UTC
    // 计算方法：System.currentTimeMillis() 或一个特定日期转毫秒
    private final long twepoch = 1577836800000L; // 2020-01-01 00:00:00 UTC

    // 各部分的位数
    private final long workerIdBits = 5L;       // 机器 ID 占 5 位 (0-31)
    private final long datacenterIdBits = 5L;   // 数据中心 ID 占 5 位 (0-31)
    private final long sequenceBits = 12L;      // 序列号占 12 位 (0-4095)，满足每毫秒 4096 个 ID

    // 各部分最大值
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long maxSequence = -1L ^ (-1L << sequenceBits);

    // 各部分左移位
    private final long workerIdShift = sequenceBits; // 机器 ID 左移 12 位
    private final long datacenterIdShift = sequenceBits + workerIdBits; // 数据中心 ID 左移 12+5=17 位
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits; // 时间戳左移 12+5+5=22 位

    private long workerId;       // 机器 ID (0-31)
    private long datacenterId;   // 数据中心 ID (0-31)
    private long sequence = 0L;  // 序列号 (0-4095)
    private long lastTimestamp = -1L; // 上次生成 ID 的时间戳

    /**
     * 构造函数
     *
     * @param datacenterId 数据中心 ID (0-31)
     * @param workerId     机器 ID (0-31)
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter ID can't be greater than %d or less than 0", maxDatacenterId));
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID can't be greater than %d or less than 0", maxWorkerId));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 生成下一个唯一的 ID
     *
     * @return 64 位雪花 ID (Long)
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上次生成 ID 的时间，说明时钟回拨过，抛出异常。
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果在同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & maxSequence; // 序列号递增
            // 当前毫秒的序列号已用完，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒，序列号重置为 0
            sequence = 0L;
        }

        lastTimestamp = timestamp; // 更新上次生成 ID 的时间戳

        // 组合各部分生成最终的 ID
        return ((timestamp - twepoch) << timestampLeftShift) // 时间戳部分
                | (datacenterId << datacenterIdShift)       // 数据中心 ID 部分
                | (workerId << workerIdShift)               // 机器 ID 部分
                | sequence;                                 // 序列号部分
    }

    /**
     * 等待直到下一毫秒
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳 (毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    // --- 示例用法 ---
    public static void main(String[] args) {
        // 创建一个雪花 ID 生成器实例
        // 传入你的数据中心 ID (0-31) 和机器 ID (0-31)
        // 例如：数据中心ID为0，机器ID为0
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0, 0);

        long start = System.currentTimeMillis();
        int count = 0;
        int targetCount = 100000; // 目标生成 100000 个 ID，看看需要多久

        System.out.println("开始生成 " + targetCount + " 个 ID...");
        for (int i = 0; i < targetCount; i++) {
            idGenerator.nextId();
            count++;
        }
        long end = System.currentTimeMillis();
        long duration = end - start;

        System.out.println("生成 " + count + " 个 ID 耗时: " + duration + " 毫秒");
        System.out.println("平均每秒生成: " + (double) count / (duration / 1000.0) + " 个 ID");

        System.out.println("\n--- 验证 ID 示例 ---");
        for (int i = 0; i < 5; i++) {
            System.out.println("ID: " + idGenerator.nextId());
        }
    }
}