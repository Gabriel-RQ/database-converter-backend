package com.gabrielrq.database_converter.dto.etl;

import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;

import java.util.UUID;

public record ValidationRequestDTO(
        UUID id,
        DbConnectionConfigDTO originConfig,
        DbConnectionConfigDTO targetConfig
) {
}
