package com.gabrielrq.database_converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StartMigrationRequestDTO(
        @Schema(description = "Nome identificador da migração", example = "migration-test")
        String name,
        @Schema(description = "Banco de dados alvo", example = "POSTGRES")
        String target,
        @Schema(description = "Configurações de conexão da origem")
        DbConnectionConfigDTO originConfig,
        @Schema(description = "Configurações de conexão do destino")
        DbConnectionConfigDTO targetConfig
) {

}
