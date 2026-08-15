package com.jay.aicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;

/**
 * 密码工具类
 * 提供密码加密和校验功能
 *
 * @author jay
 */
public class PasswordUtil {

    /**
     * 加密盐值（建议在生产环境中通过配置文件配置）
     */
    private static final String SALT = "ai_code_mother_salt";

    /**
     * 加密密码
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encrypt(String password) {
        return DigestUtil.md5Hex(SALT + password + SALT);
    }

    /**
     * 校验密码
     *
     * @param rawPassword     原始密码
     * @param encryptedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String rawPassword, String encryptedPassword) {
        return encrypt(rawPassword).equals(encryptedPassword);
    }
}
