package com.trade.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;
import java.util.UUID;

public class RandomUtils {
    /**
     * 最基础的实现，使用 Random 类从预定义的字符集中随机选取字符，生成指定长度的字符串。
     * @param maxLength
     * @return
     */
    public static String generateRandomString(int maxLength) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        int length = random.nextInt(maxLength + 1); // 随机长度 0 到 maxLength
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    /**
     * 利用 UUID 生成随机字符串，适合需要唯一性的场景，截取前 0-6 位。
     * @param maxLength
     * @return
     */
    public static String randomUidString(int maxLength) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        int length = (int) (Math.random() * (maxLength + 1)); // 随机长度
        return uuid.substring(0, Math.min(length, uuid.length()));
    }

    public static String randomStringLang(int maxLength) {
        int length = (int) (Math.random() * (maxLength + 1)); // 随机长度
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
