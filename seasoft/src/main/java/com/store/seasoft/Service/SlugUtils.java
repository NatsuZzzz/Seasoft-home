package com.store.seasoft.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

// "Thiết kế Website chuẩn SEO!" -> "thiet-ke-website-chuan-seo"
public final class SlugUtils {

    private static final int MAX = 200;

    private SlugUtils() {
    }

    public static String slugify(String text) {
        String s = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")      // bo dau
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (s.length() > MAX) {
            s = s.substring(0, MAX).replaceAll("-+$", "");
        }
        return s.isEmpty() ? "bai-viet" : s;
    }

    // Them hau to -2, -3... neu slug da ton tai
    public static String unique(String base, Predicate<String> exists) {
        String slug = base;
        for (int i = 2; exists.test(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }
}
