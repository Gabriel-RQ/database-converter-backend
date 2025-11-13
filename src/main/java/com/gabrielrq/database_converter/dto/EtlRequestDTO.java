package com.gabrielrq.database_converter.dto;


import java.util.UUID;

public record EtlRequestDTO(
        UUID id,
        DbConnectionConfigDTO originConfig,
        DbConnectionConfigDTO targetConfig,
        String target
) {
    public EtlRequestDTO(UUID id, DbConnectionConfigDTO originConfig, String target) {
        this(id, originConfig, null, target);
    }

    public EtlRequestDTO(UUID id, DbConnectionConfigDTO originConfig, DbConnectionConfigDTO targetConfig) {
        this(id, originConfig, targetConfig, null);
    }
}
