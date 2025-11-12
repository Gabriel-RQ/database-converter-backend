package com.gabrielrq.database_converter.dto;

public record ErrorResponseDTO(int status, String message, String path) {
}
