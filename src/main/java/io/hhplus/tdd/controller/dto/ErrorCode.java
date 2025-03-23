package io.hhplus.tdd.controller.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {

    // user
    NOT_SIGH_UP_USER("U_0001", "존재하지 않는 회원입니다.");

    private final String code;
    private final String message;
}