package com.gabrielrq.database_converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponseDTO(
        @Schema(description = "Código de status HTTP")
        int status,
        @Schema(description = "Mensagem descritiva do erro")
        String message,
        @Schema(description = "Caminho da rota em que o erro ocorreu")
        String path
) {
}
