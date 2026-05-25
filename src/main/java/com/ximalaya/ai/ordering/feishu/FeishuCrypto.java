package com.ximalaya.ai.ordering.feishu;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 飞书事件订阅加解密（AES-256-CBC，与开放平台文档一致）
 */
public final class FeishuCrypto {

    private FeishuCrypto() {
    }

    public static String decrypt(String encryptKey, String cipherBase64) throws Exception {
        byte[] key = sha256(encryptKey);
        byte[] decoded = Base64.getDecoder().decode(cipherBase64);
        if (decoded.length <= 16) {
            throw new IllegalArgumentException("invalid cipher text");
        }
        byte[] iv = Arrays.copyOfRange(decoded, 0, 16);
        byte[] encrypted = Arrays.copyOfRange(decoded, 16, decoded.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    public static String encrypt(String encryptKey, String plainText) throws Exception {
        byte[] key = sha256(encryptKey);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(result);
    }

    private static byte[] sha256(String encryptKey) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(encryptKey.getBytes(StandardCharsets.UTF_8));
    }
}
