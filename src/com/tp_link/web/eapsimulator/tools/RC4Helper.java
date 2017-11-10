/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-10
 */

package com.tp_link.web.eapsimulator.tools;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4Helper {

    /**
     * Encode with RC4.
     *
     * @param origin The origin data stream.
     * @param key    The RC4 key.
     * @return A new byte array of encoded data.
     */
    public static byte[] RC4(byte[] origin, byte[] key) {
        byte[] dest = new byte[origin.length];
        return RC4(origin, dest, key);
    }

    /**
     * Encode with RC4
     *
     * @param origin The origin data stream.
     * @param dest   The dest byte array to write into.
     * @param key    The RC4 key.
     * @return The dest byte array
     */
    public static byte[] RC4(byte[] origin, byte[] dest, byte[] key) {
        return RC4(origin, dest, 0, 0, origin.length, key);
    }

    /**
     * Encode with RC4
     *
     * @param origin       The origin data stream.
     * @param dest         The dest byte array
     * @param originOffset The start position of origin data encrypted.
     * @param destOffset   The start position of dest to write.
     * @param length       The size to run RC4
     * @param key          The RC4 key
     * @return The dest byte array
     */
    public static byte[] RC4(byte[] origin, byte[] dest, int originOffset, int destOffset, int length, byte[] key) {
        if (destOffset + length > dest.length || originOffset + length > origin.length) {
            return null;
        }
        int[] s = ksa(key);
        return prga(s, origin, dest, originOffset, destOffset, length);
    }

    private static int[] ksa(byte[] key) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }

        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (0xff & key[i % key.length])) % 256;
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
        }
        return s;
    }

    private static byte[] prga(int[] s, byte[] stream, byte[] dest, int originOffset, int destOffset, int length) {
        int i = 0;
        int j = 0;

        for (int l = destOffset; l < length + destOffset; l++) {
            i = (i + 1) % 256;
            j = (j + s[i]) % 256;

            int t = s[i];
            s[i] = s[j];
            s[j] = t;

            byte b;
            b = (byte) (s[(s[i] + s[j]) % 256] & 0xff);
            b = (byte) (stream[l - destOffset + originOffset] ^ b);
            dest[l] = b;
        }
        return dest;
    }

}
