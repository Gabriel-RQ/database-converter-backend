package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.TableDefinition;
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonServiceTest {

    private JsonService jsonService;

    @TempDir
    Path tempDir;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    @BeforeEach
    void setUp() {
        jsonService = new JsonService();
        ReflectionTestUtils.setField(jsonService, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(jsonService, "conversionMapsPath", "maps/");
    }

    @Test
    @DisplayName("Deve escrever um objeto em arquivo JSON")
    void shouldWriteObjectToJson() throws IOException {
        // Arrange
        String filename = "test_data";
        Map<String, String> data = Map.of("key", "value");

        // Act
        jsonService.write(data, filename);
        Path filePath = tempDir.resolve(filename + ".json");

        // Assert
        assertThat(Files.exists(filePath)).isTrue();
        String content = Files.readString(filePath);
        assertThat(content).contains("\"key\" : \"value\"");
    }

    @Test
    @DisplayName("Deve ler dados de tabela de um arquivo JSON")
    void shouldReadTableData() throws IOException {
        // Arrange
        Path tablePath = tempDir.resolve("table.json");
        Files.writeString(tablePath, "[{\"col1\":\"val1\"}]");

        // Act
        List<Map<String, Object>> result = jsonService.readTableData(tablePath);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsEntry("col1", "val1");
    }

    @Test
    @DisplayName("Deve converter ResultSet para arquivo JSON")
    void shouldWriteStreamToJson() throws SQLException, IOException {
        // Arrange
        String filename = "stream_output";
        TableDefinition table = new TableDefinition("users", new ArrayList<>(), null, null, null, null);

        when(resultSet.next()).thenReturn(true).thenReturn(false); // 1 linha
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);

        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(resultSet.getObject(1)).thenReturn(10);

        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(resultSet.getObject(2)).thenReturn("Gabriel");

        // Act
        jsonService.writeStream(resultSet, filename, table);

        // Assert
        Path filePath = tempDir.resolve(filename + ".json");
        assertThat(Files.exists(filePath)).isTrue();

        String content = Files.readString(filePath);
        assertThat(content).contains("\"id\" : 10");
        assertThat(content).contains("\"name\" : \"Gabriel\"");
    }
}