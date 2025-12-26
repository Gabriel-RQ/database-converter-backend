package com.gabrielrq.database_converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SqlDTO(
        @Schema(description = "Nome do arquivo SQL", example = "table.sql")
        String filename,
        @Schema(description = "Conteúdo do arquivo SQL", example = "CREATE TABLE table (...);")
        String content
) {
}
