package com.gabrielrq.database_converter.service.etl;


import com.gabrielrq.database_converter.domain.builder.ColumnDefinitionBuilder;
import com.gabrielrq.database_converter.domain.builder.ForeignKeyDefinitionBuilder;
import com.gabrielrq.database_converter.domain.builder.TableDefinitionBuilder;
import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.service.DatabaseConnectionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class DataExtractionService {

    private final DatabaseConnectionService databaseConnectionService;

    public DataExtractionService(DatabaseConnectionService databaseConnectionService) {
        this.databaseConnectionService = databaseConnectionService;
    }

    private void storeToJSON(JdbcTemplate template, DatabaseDefinition metadata) {

    }


    private DatabaseDefinition parseMetadata(String dbName, Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        var catalog = connection.getCatalog();
        var schema = connection.getSchema();

        List<TableDefinition> tables = new ArrayList<>();

        // Tables
        try (ResultSet tableRs = metadata.getTables(catalog, schema, null, new String[]{"TABLE"})) {
            while (tableRs.next()) {
                String tableName = tableRs.getString("TABLE_NAME");
                List<String> tablePkCols = new ArrayList<>();

                TableDefinitionBuilder table = new TableDefinitionBuilder()
                        .setName(tableName)
                        .setSchema(tableRs.getString("TABLE_SCHEMA"));


                // Primary Keys
                try (ResultSet pkRs = metadata.getPrimaryKeys(catalog, schema, tableName)) {
                    while (pkRs.next()) {
                        tablePkCols.add(pkRs.getString("COLUMN_NAME"));
                    }
                }
                table.setPrimaryKeyColumns(tablePkCols);

                // Columns
                try (ResultSet colRs = metadata.getColumns(catalog, schema, tableName, null)) {
                    while (colRs.next()) {
                        table.addColumn(
                                new ColumnDefinitionBuilder()
                                        .setName(colRs.getString("COLUMN_NAME"))
                                        .setGenericType(colRs.getInt("DATA_TYPE"))
                                        .setOriginType(colRs.getString("TYPE_NAME"))
                                        .setTargetType(null)
                                        .setDefaultValue(colRs.getString("COLUMN_DEF"))
                                        .setLength(colRs.getInt("COLUMN_SIZE"))
                                        .setPrecision(colRs.getInt("COLUMN_SIZE"))
                                        .setScale(colRs.getInt("DECIMAL_DIGITS"))
                                        .setNullable(colRs.getString("IS_NULLABLE").equalsIgnoreCase("YES"))
                                        .setAutoIncrement(colRs.getString("IS_AUTOINCREMENT").equalsIgnoreCase("YES"))
                                        .setOrdinalPosition(colRs.getInt("ORDINAL_POSITION"))
                                        .build()
                        );
                    }
                }

                // Foreign Keys
                Map<String, ForeignKeyDefinitionBuilder> fkMap = new LinkedHashMap<>();
                try (ResultSet fkRs = metadata.getImportedKeys(catalog, schema, tableName)) {
                    while (fkRs.next()) {
                        String fkName = fkRs.getString("FK_NAME");
                        String pkTableName = fkRs.getString("PKTABLE_NAME");
                        String fkColumn = fkRs.getString("FKCOLUMN_NAME");
                        String pkColumn = fkRs.getString("PKCOLUMN_NAME");

                        fkMap.computeIfAbsent(fkName, name -> new ForeignKeyDefinitionBuilder(name, pkTableName))
                                .addColumnPair(fkColumn, pkColumn);
                    }
                    table.setForeignKeys(
                            fkMap.values().stream()
                                    .map(ForeignKeyDefinitionBuilder::build)
                                    .toList()
                    );
                }

                // Unique constraints
                Map<String, List<String>> constraintMap = new LinkedHashMap<>();
                try (ResultSet uniqueRs = metadata.getIndexInfo(catalog, schema, tableName, true, false)) {
                    while (uniqueRs.next()) {
                        if (uniqueRs.getBoolean("NON_UNIQUE")) continue;

                        String indexName = uniqueRs.getString("INDEX_NAME");
                        String columnName = uniqueRs.getString("COLUMN_NAME");

                        if (indexName == null || columnName == null || tablePkCols.contains(columnName))
                            continue;

                        constraintMap.computeIfAbsent(indexName, k -> new ArrayList<>())
                                .add(columnName);
                    }
                    table.setUniqueConstraints(constraintMap.values().stream().toList());
                }

                tables.add(table.build());
            }
        }

        return new DatabaseDefinition(
                dbName,
                schema,
                tables,
                Optional.ofNullable(connection.getClientInfo("characterEncoding")).orElse("utf-8")
        );
    }

    public DatabaseDefinition extract(DbConnectionConfigDTO config) {
        try (Connection connection = databaseConnectionService.createConnection(config)) {
            var metadata = parseMetadata(config.name(), connection);
            storeToJSON(databaseConnectionService.createJdbcTemplate(config), metadata);
            return metadata;
        } catch (SQLException e) {
            throw new RuntimeException(); // lançar excessão customizada que será tratada pela aplicação
        }
    }

}
