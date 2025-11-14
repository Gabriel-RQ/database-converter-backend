package com.gabrielrq.database_converter.dto.etl;

import java.util.UUID;

public record TransformationRequestDTO(
        UUID id,
        String target
) {
}
