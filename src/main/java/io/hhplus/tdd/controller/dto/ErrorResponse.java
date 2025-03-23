package io.hhplus.tdd.controller.dto;

public record ErrorResponse(
        String code,
        String message
) {
}
