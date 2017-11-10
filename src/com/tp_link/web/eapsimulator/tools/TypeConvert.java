/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-26
 */

package com.tp_link.web.eapsimulator.tools;

public class TypeConvert {
    /**
     * Convert Int to byte array.
     *
     * @param value  The Integer to convert.
     * @param dst    The destination byte array to write in.
     * @param offset The position to start write in.
     * @return The dst param.
     */
    public static byte[] intToBytes(int value, byte[] dst, int offset) {
        if (dst != null && dst.length >= offset + 4) {
            dst[offset + 3] = (byte) value;
            value = value >> 8;
            dst[offset + 2] = (byte) value;
            value = value >> 8;
            dst[offset + 1] = (byte) value;
            value = value >> 8;
            dst[offset] = (byte) value;
        }
        return dst;
    }

    /**
     * Convert int to byte array.
     *
     * @param value The Integer to convert.
     * @return A new byte array, length is 4.
     */
    public static byte[] intToBytes(int value) {
        byte[] res = new byte[4];
        return intToBytes(value, res, 0);
    }

    /**
     * Convert byte array to Integer.
     *
     * @param bytes  The origin bytes.
     * @param offset the position of byte array to start convert.
     * @return A integer.
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        if (offset + 4 > bytes.length) {
            throw new ArrayIndexOutOfBoundsException("Out of bound converting byte array to int, from " + offset + "!");
        }
        return ((bytes[offset] & 0xff) << 24) + ((bytes[offset + 1] & 0xff) << 16)
                + ((bytes[offset + 2] & 0xff) << 8) + (bytes[offset + 3] & 0xff);
    }

    /**
     * Concert byte array to Integer.
     *
     * @param bytes The origin bytes.
     * @return A integer.
     */
    public static int byteToInt(byte[] bytes) {
        return bytesToInt(bytes, 0);
    }
}
