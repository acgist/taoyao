package com.acgist.taoyao.signal.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.config.SocketProperties.Encrypt;

import lombok.extern.slf4j.Slf4j;

/**
 * 加密工具
 * 
 * @author acgist
 */
@Slf4j
public class CipherUtils {

    private CipherUtils() {
    }
    
    /**
     * @param mode 模式
     * @param encrypt 算法
     * @param key 密钥
     * 
     * @return 加密工具
     */
    public static final Cipher buildCipher(int mode, Encrypt encrypt, String key) {
        if(encrypt == null || encrypt == Encrypt.PLAINTEXT || StringUtils.isEmpty(key)) {
            return null;
        }
        try {
            final String algo = encrypt.getAlgo();
            final String name = encrypt.name();
            final Cipher cipher = Cipher.getInstance(algo);
            cipher.init(mode, new SecretKeySpec(Base64.getMimeDecoder().decode(key), name));
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            log.error("创建加密工具异常：{} - {} - {}", mode, encrypt, key, e);
        }
        return null;
    }
    
}
