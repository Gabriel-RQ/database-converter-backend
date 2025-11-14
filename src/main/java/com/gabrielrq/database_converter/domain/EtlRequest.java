package com.gabrielrq.database_converter.domain;


import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;

import java.util.UUID;

public record EtlRequest(
        UUID id,
        DbConnectionConfigDTO originConfig,
        DbConnectionConfigDTO targetConfig,
        String target
) {
}
