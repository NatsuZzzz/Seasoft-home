package com.store.seasoft.Service;

import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

// Ban Java cua Blog.render() trong Html/js/blog.js - PHAI giu 2 ban cung quy tac:
// escape truoc, "## " -> h2, "### " -> h3, "- " -> danh sach, dong trong -> doan moi.
public final class BlogRenderer {

    private BlogRenderer() {
    }

    public static String render(String text) {
        List<String> out = new ArrayList<>();
        List<String> para = new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String line : (text == null ? "" : text).split("\\r?\\n", -1)) {
            String t = line.trim();
            if (t.isEmpty()) {
                flushPara(out, para);
                flushList(out, list);
            } else if (t.startsWith("### ")) {
                flushPara(out, para);
                flushList(out, list);
                out.add("<h3>" + esc(t.substring(4)) + "</h3>");
            } else if (t.startsWith("## ")) {
                flushPara(out, para);
                flushList(out, list);
                out.add("<h2>" + esc(t.substring(3)) + "</h2>");
            } else if (t.startsWith("- ")) {
                flushPara(out, para);
                list.add(t.substring(2));
            } else {
                flushList(out, list);
                para.add(t);
            }
        }
        flushPara(out, para);
        flushList(out, list);
        return String.join("\n", out);
    }

    public static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s, "UTF-8");
    }

    private static void flushPara(List<String> out, List<String> para) {
        if (!para.isEmpty()) {
            out.add("<p>" + String.join("<br>", para.stream().map(BlogRenderer::esc).toList()) + "</p>");
            para.clear();
        }
    }

    private static void flushList(List<String> out, List<String> list) {
        if (!list.isEmpty()) {
            out.add("<ul>" + String.join("", list.stream().map(li -> "<li>" + esc(li) + "</li>").toList()) + "</ul>");
            list.clear();
        }
    }
}
