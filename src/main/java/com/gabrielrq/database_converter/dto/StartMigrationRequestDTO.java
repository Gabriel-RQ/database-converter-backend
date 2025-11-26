package com.gabrielrq.database_converter.dto;

public record StartMigrationRequestDTO(
        String name,
        String target,
        DbConnectionConfigDTO originConfig,
        DbConnectionConfigDTO targetConfig
) {

}
