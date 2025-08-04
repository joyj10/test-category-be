package com.musinsa.shop.common.response;

import com.musinsa.shop.common.exception.ExceptionCode;
import lombok.Getter;

@Getter
public class ResultResponse<T> {
    private String code;
    private String message;
    private T data;

    public ResultResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultResponse<T> success(T data) {
        return new ResultResponse<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> ResultResponse<T> success() {
        return success(null);
    }

    public static <T> ResultResponse<T> of(String code, String message, T data) {
        return new ResultResponse<>(code, message, data);
    }

    public static <T> ResultResponse<T> of(ExceptionCode exceptionCode, String message) {
        return new ResultResponse<>(exceptionCode.getCode(), message, null);
    }

    public static <T> ResultResponse<T> of(ExceptionCode exceptionCode) {
        return new ResultResponse<>(exceptionCode.getCode(), exceptionCode.getMessage(), null);
    }
}
