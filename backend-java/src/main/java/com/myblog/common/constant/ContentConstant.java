package com.myblog.common.constant;

import java.util.regex.Pattern;

/**
 * 内容领域共享常量。
 */
public final class ContentConstant {

    private ContentConstant() {
    }

    /** MyLab 文章标识（post_key）的合法格式：字母或数字开头，允许 . _ -，最长 96 字符。 */
    public static final Pattern POST_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$");
}
