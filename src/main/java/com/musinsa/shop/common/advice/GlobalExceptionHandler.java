package com.musinsa.shop.common.advice;

import com.musinsa.shop.common.exception.DuplicateResourceException;
import com.musinsa.shop.common.exception.ExceptionCode;
import com.musinsa.shop.common.exception.InvalidRequestException;
import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.common.response.ResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResultResponse<Object> handleException(Exception e) {
      log.error("handleException: {}", e.getMessage(), e);
      return ResultResponse.of(ExceptionCode.SERVER_ERROR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException.class)
    public ResultResponse<Object> handleInvalidRequestException(InvalidRequestException e) {
        log.error("handleInvalidRequestException: {}", e.getMessage(), e);
        return ResultResponse.of(ExceptionCode.INVALID_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultResponse<Object> handleValidationException(MethodArgumentNotValidException e) {
        log.error("handleValidationException: {}", e.getMessage(), e);

        String errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResultResponse.of(ExceptionCode.INVALID_REQUEST, errorMessages);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResultResponse<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("handleMissingServletRequestParameterException: {}", e.getMessage(), e);
        return ResultResponse.of(ExceptionCode.INVALID_REQUEST, "필수 요청 파라미터가 누락되었습니다: " + e.getParameterName());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultResponse<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("handleHttpMessageNotReadableException: {}", e.getMessage(), e);
        return ResultResponse.of(ExceptionCode.INVALID_REQUEST, "요청 본문(JSON)을 읽을 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultResponse<Object> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("handleMethodNotSupportedException: {}", e.getMessage(), e);
        return ResultResponse.of(ExceptionCode.METHOD_NOT_ALLOWED);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResultResponse<Object> handleNotFoundException(ResourceNotFoundException e) {
        log.error("handleBadRequestException: {}", e.getMessage(), e);
        return ResultResponse.of(ExceptionCode.RESOURCE_NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateResourceException .class)
    public ResultResponse<Object> handleDuplicateException(DuplicateResourceException e) {
        log.error("handleDuplicateException: {}", e.getMessage(), e);
        return ResultResponse.of(ExceptionCode.DUPLICATE_RESOURCE);
    }
}
