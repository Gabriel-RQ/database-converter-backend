package com.gabrielrq.database_converter.domain.builder;

import com.gabrielrq.database_converter.domain.ColumnDefinition;
import com.gabrielrq.database_converter.domain.ForeignKeyDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;

import java.util.ArrayList;
import java.util.List;

public class TableDefinitionBuilder {
    private String name;
    private List<ColumnDefinition> columns = new ArrayList<>();
    private List<String> primaryKeyColumns = new ArrayList<>();
    private List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
    private List<List<String>> uniqueConstraints = new ArrayList<>();
    private String schema;

    public TableDefinitionBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public TableDefinitionBuilder setColumns(List<ColumnDefinition> columns) {
        this.columns = columns;
        return this;
    }

    public TableDefinitionBuilder addColumn(ColumnDefinition column) {
        this.columns.add(column);
        return this;
    }

    public TableDefinitionBuilder setPrimaryKeyColumns(List<String> primaryKeyColumns) {
        this.primaryKeyColumns = primaryKeyColumns;
        return this;
    }

    public TableDefinitionBuilder addPrimaryKeyColumn(String columnName) {
        this.primaryKeyColumns.add(columnName);
        return this;
    }

    public TableDefinitionBuilder setForeignKeys(List<ForeignKeyDefinition> foreignKeys) {
        this.foreignKeys = foreignKeys;
        return this;
    }

    public TableDefinitionBuilder addForeignKey(ForeignKeyDefinition fk) {
        this.foreignKeys.add(fk);
        return this;
    }

    public TableDefinitionBuilder setUniqueConstraints(List<List<String>> uniqueConstraints) {
        this.uniqueConstraints = uniqueConstraints;
        return this;
    }

    public TableDefinitionBuilder addUniqueConstraint(List<String> constraint) {
        this.uniqueConstraints.add(constraint);
        return this;
    }

    public TableDefinitionBuilder setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public static TableDefinitionBuilder from(TableDefinition table) {
        return new TableDefinitionBuilder()
                .setName(table.name())
                .setColumns(table.columns())
                .setSchema(table.schema())
                .setForeignKeys(table.foreignKeys())
                .setUniqueConstraints(table.uniqueConstraints())
                .setPrimaryKeyColumns(table.primaryKeyColumns());
    }

    public TableDefinition build() {
        return new TableDefinition(
                name,
                columns,
                primaryKeyColumns,
                foreignKeys,
                uniqueConstraints,
                schema
        );
    }
}
