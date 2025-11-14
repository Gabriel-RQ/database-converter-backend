package com.gabrielrq.database_converter.dto.etl;

import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;

import java.util.UUID;

public record ExtractionRequestDTO(
        UUID id,
        DbConnectionConfigDTO originConfig
) {
}
