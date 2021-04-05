package com.izhiliu.erp.common;

import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import org.apache.commons.lang.StringUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: louis
 * @Date: 2019/10/31 14:48
 */
public class CommonUtils {

    public static final String SUM = "SUM";
    public static final String SUBTRACTION = "SUBTRACTION";
    public static final String MULTIPLY = "MULTIPLY";
    public static final String DIVIDE = "DIVIDE";

    public static boolean isNotBlank(Object object) {
        if (null == object) {
            return false;
        }
        if (object instanceof String) {
            return StringUtils.isNotBlank(String.valueOf(object));
        } else if (object instanceof List) {
            return null != object && !((List) object).isEmpty();
        } else if (object instanceof Map) {
            return null != object && !((Map) object).isEmpty();
        } else if (object instanceof Set) {
            return ((Set) object).isEmpty();
        } else if (object instanceof String[]) {
            return ((String[]) object).length > 0;
        } else if (object instanceof Integer[]) {
            return object != null;
        } else if (object instanceof Long) {
            return object != null;
        } else if (object instanceof Integer) {
            return object != null;
        } else {
            return object != null;
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static Float getFloatSum(Float num1, Float num2) {
        BigDecimal bigDecimal1 = new BigDecimal(Float.toString(num1));
        BigDecimal bigDecimal2 = new BigDecimal(Float.toString(num2));
        BigDecimal result = bigDecimal1.add(bigDecimal2);
        return result.floatValue();
    }

    public static Float getFloatByBigDecimal(Float num1, Float num2, String type) {
        BigDecimal bigDecimal1 = new BigDecimal(Float.toString(num1));
        BigDecimal bigDecimal2 = new BigDecimal(Float.toString(num2));
        BigDecimal result = new BigDecimal(BigInteger.ZERO);
        switch (type) {
            case SUM:
                //求和
                result = bigDecimal1.add(bigDecimal2);
                break;
            case SUBTRACTION:
                // 减法
                result = bigDecimal1.subtract(bigDecimal2);
                break;
            case MULTIPLY:
                //乘法
                result = bigDecimal1.multiply(bigDecimal2);
                break;
            case DIVIDE:
                //除法 保留两位小数
                result = bigDecimal1.divide(bigDecimal2, 2, BigDecimal.ROUND_CEILING);
                break;
        }
        return result.floatValue();
    }

    public static boolean isBlank(Object object) {
        return !isNotBlank(object);
    }


    /**
     * 截取中文
     * @param str
     * @return
     */
    public static String getSkuCode(String str) {
        if (isContainChinese(str)) {
            char[] chars = str.toCharArray();
            StringBuffer result = new StringBuffer();
            for (char aChar : chars) {
                if (!isChinese(aChar)) {
                    result.append(aChar);
                }
            }
            return result.toString();
        }
        return str;
    }

    /**
     *  只截取【字母数字 -:_+=,.】
     * @param str
     * @return
     */
    public static String getSkuCodeWord(String str) {
        if (CommonUtils.isNotBlank(str)) {
            Pattern pattern = Pattern.compile("[^a-zA-Z0-9-:_+=,.]");
            Matcher matcher = pattern.matcher(str);
            StringBuffer buffer = new StringBuffer();
            // 如果找到非法字符
            while (matcher.find()) {
                // 如果里面包含非法字符如冒号双引号等，那么就把他们消去，并把非法字符前面的字符放到缓冲区
                matcher.appendReplacement(buffer, "");
            }
            // 将剩余的合法部分添加到缓冲区
            matcher.appendTail(buffer);
            return buffer.toString();
        }
        return str;
    }


    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
            || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    private static Boolean isContainChinese(String str) {
        Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = pattern.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    /**
     * 处理shopee api结果
     * @param shopeeResult
     * @return
     */
    public static Object handleShopeeApiResult(ShopeeResult shopeeResult) {
        if (isNotBlank(shopeeResult.getError())) {
            throw new LuxServerErrorException(shopeeResult.getError().getMsg());
        }
        return shopeeResult.getData();
    }


    public static String getShortUUID(Integer number) {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, number).toUpperCase();
    }

    /**
     * 获取任务id
     * @param login
     * @return
     */
    public static String getTaskId(String login) {
        String concat = login.concat(getShortUUID(6));
        return new BASE64Encoder().encode(concat.getBytes());
    }

    /**
     *  解析任务id,只有login匹配时候才能获取真正的任务id
     * @param key
     * @param login
     * @return
     */
    public static String decodeTaskId(String key, String login) {
        try {
            byte[] bytes = new BASE64Decoder().decodeBuffer(key);
            String decode = new String(bytes, "UTF-8");
            return decode.contains(login) ? decode : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * String 转 Long
     * @param string
     * @return
     */
    public static Long getStrLong(String string) {
        if (isBlank(string)) {
            return null;
        }
        return Long.parseLong(string);
    }

    /**
     * Long 转 String
     * @param num
     * @return
     */
    public static String getStrByLong(Long num) {
        if (isBlank(num)) {
            return null;
        }
        return num.toString();
    }

    /**
     * Object转Str
     * @param object
     * @return
     */
    public static String objectToStr(Object object) {
        if (isBlank(object)) {
            return null;
        }
        return object.toString();
    }

    /**
     * shopee商品库存最高限制: 999998
     * @param stock
     * @return
     */
    public static int getMaxStock(int stock) {
        if (isBlank(stock)) {
            return 0;
        }
        return stock > 999998 ? 999998 : stock;
    }

}
