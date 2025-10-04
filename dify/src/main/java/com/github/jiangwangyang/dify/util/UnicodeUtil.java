package com.github.jiangwangyang.dify.util;

public final class UnicodeUtil {

    /**
     * 把 \\uXXXX 转回真实字符
     * 只处理 \\uXXXX 形式，其它原文不动
     */
    public static String unescape(String src) {
        if (src == null || !src.contains("\\u")) {
            return src;
        }
        StringBuilder sb = new StringBuilder(src.length());
        char[] cs = src.toCharArray();
        for (int i = 0; i < cs.length; ) {
            if (cs[i] == '\\' && i + 5 < cs.length && cs[i + 1] == 'u') {
                // 取出 4 位 16 进制
                int hex = Integer.parseInt(new String(cs, i + 2, 4), 16);
                sb.append((char) hex);
                i += 6;
            } else {
                sb.append(cs[i++]);
            }
        }
        return sb.toString();
    }
}
