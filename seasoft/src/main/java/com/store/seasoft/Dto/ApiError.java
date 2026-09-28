package com.store.seasoft.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

// Format loi chung cua moi API: { "status": 400, "message": "...", "errors": { "email": "..." } }
// "errors" chi co khi loi validate tung truong.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> errors) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null);
    }
}
