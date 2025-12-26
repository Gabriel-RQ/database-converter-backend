package com.gabrielrq.database_converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DbConnectionConfigDTO(
        @Schema(description = "Nome ou caminho da base de dados")
        String name,
        @Schema(description = "URL JDBC da base de dados", example = "jdbc:postgresql://localhost:5432/pagila")
        String jdbcUrl,
        @Schema(description = "Nome de usuário do SGBD")
        String username,
        @Schema(description = "Senha de acesso ao SGBD")
        String password,
        @Schema(description = "Caminho do driver SGBD da base de dados", example = "org.postgresql.Driver")
        String driverClassName
) {
}
