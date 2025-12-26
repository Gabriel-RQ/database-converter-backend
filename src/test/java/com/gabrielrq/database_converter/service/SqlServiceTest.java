package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.dto.SqlDTO;
import com.gabrielrq.database_converter.dto.SqlPageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqlServiceTest {

    @Mock
    private JsonService jsonService;

    @Mock
    private Statement statement;

    private SqlService sqlService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        sqlService = new SqlService(jsonService);
        ReflectionTestUtils.setField(sqlService, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(sqlService, "ddlPath", "ddl");
        ReflectionTestUtils.setField(sqlService, "dmlPath", "dml");
    }

    @Test
    @DisplayName("Deve escrever e ler um arquivo corretamente")
    void shouldWriteAndReadFile() throws IOException {
        // Arrange
        Path relativePath = Path.of((String) Objects.requireNonNull(ReflectionTestUtils.getField(sqlService, "basePath"))).resolve("test/file.txt");
        String content = "Test content";

        // Act
        sqlService.write(relativePath, content);
        String readContent = sqlService.read(relativePath);

        // Assert
        assertThat(readContent).isEqualTo(content);
        assertThat(Files.exists(tempDir.resolve(relativePath))).isTrue();
    }

    @Test
    @DisplayName("Deve listar arquivos DDL paginados")
    void shouldListDDLFilesPaginated() throws IOException {
        // Arrange
        Path ddlDir = tempDir.resolve("migration-id").resolve("ddl");
        Files.createDirectories(ddlDir);
        Files.writeString(ddlDir.resolve("1_table.sql"), "CREATE TABLE 1...");
        Files.writeString(ddlDir.resolve("2_table.sql"), "CREATE TABLE 2...");
        Files.writeString(ddlDir.resolve("3_table.sql"), "CREATE TABLE 3...");

        // Act
        SqlPageDTO result = sqlService.listDDL("migration-id", 0, 2);

        // Assert
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.files()).hasSize(2);
        assertThat(result.files().getFirst().filename()).isEqualTo("1_table.sql");
    }

    @Test
    @DisplayName("Deve executar blocos SQL lidos de arquivo bufferizado")
    void shouldBufferReadAndExecSql() throws IOException, java.sql.SQLException {
        // Arrange
        Path sqlFile = tempDir.resolve("script.sql");
        String sqlContent = """
                INSERT INTO table VALUES (1);
                INSERT INTO table VALUES (2);
                -- Ignored comment
                UPDATE table SET col = 1;
                """;
        Files.writeString(sqlFile, sqlContent);

        // Act
        sqlService.bufferReadAndExec(Path.of("script.sql"), statement);

        // Assert
        verify(statement, times(3)).execute(anyString());
    }
}