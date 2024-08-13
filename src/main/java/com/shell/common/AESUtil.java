package com.shell.common;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.logging.Level;


@UtilityClass
@Log
public class AESUtil {


    public String encrypt(String text, String key) {
        var iv = new byte[16];
        return encrypt(text, key, iv);
    }


    public String decrypt(String encryptedText, String key) {
        var iv = new byte[16];
        return decrypt(encryptedText, key, iv);
    }


    private String encrypt(String text, String key, byte[] iv) {
        try {
            var aesEngine = AESEngine.newInstance();
            var block = CBCBlockCipher.newInstance(aesEngine);
            var cipher = new PaddedBufferedBlockCipher(block);
            var keyParam = new KeyParameter(key.getBytes(StandardCharsets.UTF_8));
            ParametersWithIV keyParamWithIV = new ParametersWithIV(keyParam, iv, 0, 16);
            cipher.init(true, keyParamWithIV);
            var inputBytes = text.getBytes(StandardCharsets.UTF_8);
            var inputLength = inputBytes.length;
            var output = new byte[cipher.getOutputSize(inputLength)];
            var length = cipher.processBytes(inputBytes, 0, inputLength, output, 0);
            cipher.doFinal(output, length);
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception exception) {
            log.log(Level.WARNING, MessageFormat.format("AESUtil >> encrypt secretKey: {0}, data: {1} >> Exception:", key, text), exception);
            return "";
        }


    }


    private static String decrypt(String encryptedText, String key, byte[] iv) {
        try {
            var aesEngine = AESEngine.newInstance();
            var block = CBCBlockCipher.newInstance(aesEngine);
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(block);


            KeyParameter keyParam = new KeyParameter(key.getBytes(StandardCharsets.UTF_8));
            ParametersWithIV keyParamWithIV = new ParametersWithIV(keyParam, iv, 0, 16);
            cipher.init(false, keyParamWithIV);


            byte[] inputBytes = Base64.getDecoder().decode(encryptedText);
            int inputLength = inputBytes.length;
            byte[] output = new byte[cipher.getOutputSize(inputLength)];
            int length = cipher.processBytes(inputBytes, 0, inputLength, output, 0);
            cipher.doFinal(output, length);
            return new String(output, StandardCharsets.UTF_8).trim();
        } catch (Exception exception) {
            log.log(Level.WARNING, MessageFormat.format("AESUtil >> decrypt secretKey: {0}, data: {1} >> Exception:", key, encryptedText), exception);
            return "";
        }


    }


    @SuppressWarnings("java:S2245")
    public String generateSecretKey() {
        return RandomStringUtils.random(32, true, true);
    }


}
