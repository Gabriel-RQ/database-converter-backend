package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.ColumnDefinition;
import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.dto.ConsistencyValidationDataDTO;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.exception.ConsistencyValidationException;
import com.gabrielrq.database_converter.service.ConsistencyValidationService;
import com.gabrielrq.database_converter.service.DatabaseConnectionService;
import com.gabrielrq.database_converter.service.etl.DataExtractionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsistencyValidationServiceTest {

    @Mock
    private DataExtractionService extractionService;

    @Mock
    private Connection connection;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ConsistencyValidationService validationService;

    @Test
    @DisplayName("Deve validar com sucesso quando as bases são idênticas")
    void shouldValidateSuccessfullyWhenIdentical() throws SQLException {
        // Arrange
        String identifier = "migration-id";
        DbConnectionConfigDTO config = mock(DbConnectionConfigDTO.class);

        ColumnDefinition col = new ColumnDefinition("id", 1, "INT", "INT", null, null, null, false, false, null, 1);
        TableDefinition table = new TableDefinition("users", List.of(col), null, null, null, null);
        DatabaseDefinition metadata = new DatabaseDefinition("db", "public", List.of(table), "utf-8");

        when(extractionService.parseMetadata(any(), any())).thenReturn(metadata);

        try (MockedStatic<DatabaseConnectionService> dbServiceMock = mockStatic(DatabaseConnectionService.class)) {
            dbServiceMock.when(() -> DatabaseConnectionService.createConnection(any())).thenReturn(connection);
            dbServiceMock.when(() -> DatabaseConnectionService.createJdbcTemplate(any())).thenReturn(jdbcTemplate);

            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(100L);

            // Act
            ConsistencyValidationDataDTO result = validationService.validate(identifier, config, config);

            // Assert
            assertThat(result.isConsistent()).isTrue();
            assertThat(result.areTablesOk()).isTrue();
            assertThat(result.areColumnsOk()).isTrue();
            assertThat(result.messages()).isEmpty();
        }
    }

    @Test
    @DisplayName("Deve detectar inconsistência estrutural (tabela faltando)")
    void shouldDetectMissingTable() throws SQLException {
        String identifier = "migration-id";
        DbConnectionConfigDTO config = mock(DbConnectionConfigDTO.class);

        TableDefinition tableOrigin = new TableDefinition("users", Collections.emptyList(), null, null, null, null);
        DatabaseDefinition metaOrigin = new DatabaseDefinition("origin", "public", List.of(tableOrigin), "utf-8");
        DatabaseDefinition metaTarget = new DatabaseDefinition("target", "public", Collections.emptyList(), "utf-8");

        when(extractionService.parseMetadata(any(), any()))
                .thenReturn(metaOrigin)
                .thenReturn(metaTarget);

        try (MockedStatic<DatabaseConnectionService> dbServiceMock = mockStatic(DatabaseConnectionService.class)) {
            dbServiceMock.when(() -> DatabaseConnectionService.createConnection(any())).thenReturn(connection);
            dbServiceMock.when(() -> DatabaseConnectionService.createJdbcTemplate(any())).thenReturn(jdbcTemplate);

            // Act
            ConsistencyValidationDataDTO result = validationService.validate(identifier, config, config);

            // Assert
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.areTablesOk()).isFalse();
            assertThat(result.messages())
                    .anyMatch(msg -> msg.contains("Falha estrutural: tabela 'users' não encontrada"));
        }
    }

    @Test
    @DisplayName("Deve detectar inconsistência volumétrica (contagem de linhas diferente)")
    void shouldDetectRowCountMismatch() throws SQLException {
        String identifier = "migration-id";
        DbConnectionConfigDTO config = mock(DbConnectionConfigDTO.class);

        TableDefinition table = new TableDefinition("users", Collections.emptyList(), null, null, null, null);
        DatabaseDefinition metadata = new DatabaseDefinition("db", "public", List.of(table), "utf-8");

        when(extractionService.parseMetadata(any(), any())).thenReturn(metadata);

        try (MockedStatic<DatabaseConnectionService> dbServiceMock = mockStatic(DatabaseConnectionService.class)) {
            dbServiceMock.when(() -> DatabaseConnectionService.createConnection(any())).thenReturn(connection);
            dbServiceMock.when(() -> DatabaseConnectionService.createJdbcTemplate(any())).thenReturn(jdbcTemplate);

            when(jdbcTemplate.queryForObject(contains("users"), eq(Long.class)))
                    .thenReturn(100L)
                    .thenReturn(90L);

            // Act
            ConsistencyValidationDataDTO result = validationService.validate(identifier, config, config);

            // Assert
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.messages())
                    .anyMatch(msg -> msg.contains("Falha volumétrica"));
        }
    }

    @Test
    @DisplayName("Deve lançar exceção customizada em caso de erro SQL")
    void shouldThrowConsistencyExceptionOnError() throws SQLException {
        DbConnectionConfigDTO config = mock(DbConnectionConfigDTO.class);

        try (MockedStatic<DatabaseConnectionService> dbServiceMock = mockStatic(DatabaseConnectionService.class)) {
            dbServiceMock.when(() -> DatabaseConnectionService.createConnection(any()))
                    .thenThrow(new SQLException("Connection failed"));

            assertThrows(ConsistencyValidationException.class, () -> {
                validationService.validate("id", config, config);
            });
        }
    }
}