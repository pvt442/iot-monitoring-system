package com.demo.dto;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.demo.util.TimeUtil;

public record ErrorResponse(String timestamp, int status, String error, String message) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(TimeUtil.format(LocalDateTime.now()), status.value(), status.getReasonPhrase(), message);
    }
}
