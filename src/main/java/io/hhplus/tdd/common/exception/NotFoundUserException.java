package io.hhplus.tdd.common.exception;

import io.hhplus.tdd.controller.dto.ErrorCode;
import lombok.Getter;


@Getter
public class NotFoundUserException extends RuntimeException {

    private final ErrorCode errorCode;

    public NotFoundUserException() {
        super(ErrorCode.NOT_SIGH_UP_USER.getMessage());
        this.errorCode = ErrorCode.NOT_SIGH_UP_USER;
    }

    public NotFoundUserException(String message) {
        super(message);
        this.errorCode = ErrorCode.NOT_SIGH_UP_USER;
    }

    public NotFoundUserException(Long userId) {
        super(ErrorCode.NOT_SIGH_UP_USER.getMessage() + " (ID: " + userId + ")");
        this.errorCode = ErrorCode.NOT_SIGH_UP_USER;
    }
}