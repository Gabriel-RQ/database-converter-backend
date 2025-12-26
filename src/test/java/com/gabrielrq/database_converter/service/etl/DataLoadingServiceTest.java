package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.domain.TransformationResult;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.exception.LoadingException;
import com.gabrielrq.database_converter.service.DatabaseConnectionService;
import com.gabrielrq.database_converter.service.SqlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataLoadingServiceTest {

    @Mock
    private SqlService sqlService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @InjectMocks
    private DataLoadingService dataLoadingService;

    @Test
    @DisplayName("Deve executar carga de dados (DDL e DML) com sucesso")
    void shouldLoadDataSuccessfully() throws SQLException, IOException {
        // Arrange
        String identifier = "migration-id";
        DbConnectionConfigDTO config = new DbConnectionConfigDTO("target", "url", "user", "pass", "driver");

        TableDefinition table = new TableDefinition(
                "users", Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), "public"
        );
        TransformationResult result = new TransformationResult(
                new DatabaseDefinition("db", "public", List.of(table), "utf-8"),
                List.of(table)
        );

        when(sqlService.read(any(Path.class))).thenReturn("CREATE TABLE users...");
        when(connection.createStatement()).thenReturn(statement);

        try (MockedStatic<DatabaseConnectionService> mockedDbService = mockStatic(DatabaseConnectionService.class)) {
            mockedDbService.when(() -> DatabaseConnectionService.createJdbcTemplate(config)).thenReturn(jdbcTemplate);
            mockedDbService.when(() -> DatabaseConnectionService.createConnection(config)).thenReturn(connection);

            // Act
            dataLoadingService.load(identifier, config, result);

            // Assert
            verify(jdbcTemplate).execute("CREATE TABLE users...");

            verify(sqlService).bufferReadAndExec(any(Path.class), eq(statement));

            verify(connection).commit();
        }
    }

    @Test
    @DisplayName("Deve ignorar FileNotFoundException durante o DDL e continuar")
    void shouldIgnoreFileNotFoundInDDL() throws IOException {
        // Arrange
        String identifier = "migration-id";
        DbConnectionConfigDTO config = new DbConnectionConfigDTO("target", "url", "user", "pass", "driver");
        TableDefinition table = new TableDefinition("users", Collections.emptyList(), null, null, null, null);
        TransformationResult result = new TransformationResult(null, List.of(table));

        when(sqlService.read(any(Path.class))).thenThrow(new FileNotFoundException());

        try (MockedStatic<DatabaseConnectionService> mockedDbService = mockStatic(DatabaseConnectionService.class)) {
            mockedDbService.when(() -> DatabaseConnectionService.createJdbcTemplate(config)).thenReturn(jdbcTemplate);
            mockedDbService.when(() -> DatabaseConnectionService.createConnection(config)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);

            // Act
            dataLoadingService.load(identifier, config, result);

            // Assert
            verify(jdbcTemplate, never()).execute(anyString());
            verify(connection).createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Deve lançar LoadingException ao falhar leitura de arquivo no DDL")
    void shouldThrowExceptionOnDDLReadError() throws IOException {
        // Arrange
        String identifier = "migration-id";
        DbConnectionConfigDTO config = mock(DbConnectionConfigDTO.class);
        TableDefinition table = new TableDefinition("users", Collections.emptyList(), null, null, null, null);
        TransformationResult result = new TransformationResult(null, List.of(table));

        when(sqlService.read(any(Path.class))).thenThrow(new IOException("Disk error"));

        try (MockedStatic<DatabaseConnectionService> mockedDbService = mockStatic(DatabaseConnectionService.class)) {
            mockedDbService.when(() -> DatabaseConnectionService.createJdbcTemplate(config)).thenReturn(jdbcTemplate);

            // Act & Assert
            LoadingException exception = assertThrows(LoadingException.class, () -> {
                dataLoadingService.load(identifier, config, result);
            });

            assertThat(exception.getMessage()).contains("Erro na carga de dados");
        }
    }

    @Test
    @DisplayName("Deve lançar LoadingException ao falhar execução do DML")
    void shouldThrowExceptionOnDMLExecutionError() throws SQLException, IOException {
        // Arrange
        String identifier = "migration-id";
        DbConnectionConfigDTO config = mock(DbConnectionConfigDTO.class);
        TableDefinition table = new TableDefinition("users", Collections.emptyList(), null, null, null, null);
        TransformationResult result = new TransformationResult(null, List.of(table));

        when(sqlService.read(any(Path.class))).thenReturn("SQL");

        try (MockedStatic<DatabaseConnectionService> mockedDbService = mockStatic(DatabaseConnectionService.class)) {
            mockedDbService.when(() -> DatabaseConnectionService.createJdbcTemplate(config)).thenReturn(jdbcTemplate);
            mockedDbService.when(() -> DatabaseConnectionService.createConnection(config)).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);

            doThrow(new SQLException("SQL Syntax Error")).when(sqlService).bufferReadAndExec(any(), any());

            // Act & Assert
            LoadingException exception = assertThrows(LoadingException.class, () -> {
                dataLoadingService.load(identifier, config, result);
            });

            assertThat(exception.getMessage()).contains("Erro ao executar DML");
        }
    }
}