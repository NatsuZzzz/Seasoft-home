package com.store.seasoft;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

abstract class ContentTestSupport extends ApiTestBase {

    ResultActions send(String method, String url, String auth, String json) throws Exception {
        var rb = method.equals("PUT") ? put(url) : post(url);
        return mockMvc.perform(rb.header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    String createPost(String auth, String title, String status) throws Exception {
        return send("POST", "/api/admin/content/blog", auth, """
                {"title":"%s","excerpt":"Tóm tắt","content":"Đoạn 1\\n\\n## Mục 2\\nĐoạn 2","status":"%s"}"""
                .formatted(title, status)).andReturn().getResponse().getContentAsString();
    }
}
