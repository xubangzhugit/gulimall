package com.izhiliu.erp.common;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: louis
 * @Date: 2020/7/20 15:58
 */
public class JsoupUtils {

    public static String getMetaBy(Document document, String attribute) {
        if (CommonUtils.isBlank(document) || CommonUtils.isBlank(attribute)) {
            return null;
        }
        String cssQuery = "meta[property=" + attribute + "]";
        Elements attr = document.select(cssQuery);
        if (CommonUtils.isNotBlank(attr)) {
            Element element = attr.get(0);
            return element.attr("content");
        }
        return null;
    }

    public static String getScriptBy(Document document, String start, String end) {
        if (CommonUtils.isBlank(document) || CommonUtils.isBlank(start) || CommonUtils.isBlank(end)) {
            return null;
        }
        String script = document.select("script").stream()
                .map(Element::data)
                .filter(e -> e.contains(start))
                .findFirst()
                .orElseGet(() -> null);
        return getStrBy(script, start, end);
    }

    public static String getSubStr(String html, String start, String end) {
        String strBy = getStrBy(html, start, end);
        if (CommonUtils.isBlank(strBy)) {
            return "";
        }
        return strBy.replaceAll(end, "").replace("\"", "");
    }

    public static String getStrBy(String html, String start, String end) {
        if (CommonUtils.isBlank(html) || CommonUtils.isBlank(start) || CommonUtils.isBlank(end)) {
            return null;
        }
        String concat = start.concat("(.*?)").concat(end);
        Pattern pattern = Pattern.compile(concat);
        Matcher m = pattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(0).replace(start, ""));
        }
        return sb.toString();
    }


}
