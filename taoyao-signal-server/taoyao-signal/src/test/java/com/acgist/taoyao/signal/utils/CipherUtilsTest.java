package com.acgist.taoyao.signal.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.crypto.Cipher;

import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.SocketProperties.Encrypt;

public class CipherUtilsTest {
    
    @Test
    public void testBuildCipher() {
        final String secret = "2SPWy+TF1zM=".strip();
        assertNull(CipherUtils.buildCipher(Cipher.ENCRYPT_MODE, Encrypt.PLAINTEXT, secret));
        assertNull(CipherUtils.buildCipher(Cipher.ENCRYPT_MODE, Encrypt.DES, null));
        assertNotNull(CipherUtils.buildCipher(Cipher.ENCRYPT_MODE, Encrypt.DES, secret));
    }

}
