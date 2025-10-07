package com.gabrielrq.database_converter.dto;

public record DbConnectionConfigDTO(String jdbcUrl, String username, String password, String driverClassName) {
}
