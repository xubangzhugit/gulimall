package com.izhiliu.erp.web.rest.util;

import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;

public class ShopeeUtils {

    /**
     * text = "55b03" + md5(param.encode()).hexdigest() + "55b03";
     * return "55b03-" + md5(text.encode()).hexdigest()
     *
     * @param url
     * @return
     */
    public static String encode(String url) {
        try {
            String param = new URI(url).getQuery();
            String tempStr = "55b03" + md5(param.getBytes()) + "55b03";
            String myHash = md5(tempStr.getBytes());
            return "55b03-" + myHash;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }

    public static String md5(byte[] bs) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(bs);
        String hex = new BigInteger(1, digest.digest()).toString(16);
        // 补齐BigInteger省略的前置0
        return new String(new char[32 - hex.length()]).replace("\0", "0") + hex;
    }
}
