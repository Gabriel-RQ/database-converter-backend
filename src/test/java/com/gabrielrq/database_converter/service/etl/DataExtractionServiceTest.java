package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.service.JsonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExtractionServiceTest {

    @Mock
    private JsonService jsonService;

    @InjectMocks
    private DataExtractionService extractionService;

    @Test
    @DisplayName("Deve fazer parsing dos metadados JDBC corretamente")
    void shouldParseMetadataFromConnection() throws SQLException {
        // Arrange
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet tablesRs = mock(ResultSet.class);
        ResultSet columnsRs = mock(ResultSet.class);
        ResultSet pkRs = mock(ResultSet.class);
        ResultSet fkRs = mock(ResultSet.class);
        ResultSet indexRs = mock(ResultSet.class);

        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn("test_db");
        when(connection.getSchema()).thenReturn("public");

        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tablesRs);
        when(tablesRs.next()).thenReturn(true).thenReturn(false);
        when(tablesRs.getString("TABLE_NAME")).thenReturn("USERS");
        when(tablesRs.getString("TABLE_SCHEM")).thenReturn("public");

        when(metaData.getPrimaryKeys(any(), any(), eq("USERS"))).thenReturn(pkRs);
        when(pkRs.next()).thenReturn(true).thenReturn(false);
        when(pkRs.getString("COLUMN_NAME")).thenReturn("ID");

        when(metaData.getColumns(any(), any(), eq("USERS"), any())).thenReturn(columnsRs);
        when(columnsRs.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(columnsRs.getString("COLUMN_NAME")).thenReturn("ID").thenReturn("NAME");
        when(columnsRs.getInt("DATA_TYPE")).thenReturn(4).thenReturn(12); // INTEGER, VARCHAR
        when(columnsRs.getString("TYPE_NAME")).thenReturn("INT").thenReturn("VARCHAR");
        when(columnsRs.getString("COLUMN_DEF")).thenReturn("default");
        when(columnsRs.getString("IS_NULLABLE")).thenReturn("NO").thenReturn("YES");
        when(columnsRs.getString("IS_AUTOINCREMENT")).thenReturn("YES").thenReturn("NO");

        when(metaData.getImportedKeys(any(), any(), eq("USERS"))).thenReturn(fkRs);
        when(fkRs.next()).thenReturn(false);
        when(metaData.getIndexInfo(any(), any(), eq("USERS"), eq(true), eq(false))).thenReturn(indexRs);
        when(indexRs.next()).thenReturn(false);

        // Act
        DatabaseDefinition result = extractionService.parseMetadata("test_db", connection);

        // Assert
        assertThat(result.name()).isEqualTo("test_db");
        assertThat(result.tables()).hasSize(1);

        TableDefinition table = result.tables().getFirst();
        assertThat(table.name()).isEqualTo("USERS");
        assertThat(table.primaryKeyColumns()).containsExactly("ID");
        assertThat(table.columns()).hasSize(2);
    }
}