package com.gabrielrq.database_converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SqlPageDTO(
        @Schema(description = "Página desejada")
        int page,
        @Schema(description = "Quantia de arquivos retornados por página")
        int size,
        @Schema(description = "Total de arquivos disponíveis")
        int total,
        @Schema(description = "Lista de arquivos SQL")
        List<SqlDTO> files
) {
}
