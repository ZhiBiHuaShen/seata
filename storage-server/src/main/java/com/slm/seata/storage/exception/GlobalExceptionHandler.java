package com.slm.seata.storage.exception;

import com.slm.seata.storage.enums.ResultStatus;
import com.slm.seata.storage.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handler(Exception e) {
        log.error(e.getMessage(), e);
        return ApiResponse.failure(ResultStatus.ERROR, e.getMessage());
    }

}