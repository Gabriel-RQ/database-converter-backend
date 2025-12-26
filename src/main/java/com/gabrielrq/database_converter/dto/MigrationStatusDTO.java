package com.gabrielrq.database_converter.dto;

import com.gabrielrq.database_converter.enums.EtlStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record MigrationStatusDTO(
        @Schema(description = "ID da migração")
        UUID id,
        @Schema(description = "Nome identificador da migração", example = "migration-test")
        String name,
        @Schema(description = "Etapa atual do processo de migração")
        EtlStep step,
        @Schema(description = "Mensagem relacionada a etapa atual do processo de migração")
        String message,
        @Schema(description = "Data de inicio do processo de migração")
        LocalDateTime startedAt,
        @Schema(description = "Data de finalização do processo de migração")
        LocalDateTime finishedAt,
        @Schema(description = "Data da última atualização do processo de migração")
        LocalDateTime lastUpdatedAt
) {
}
