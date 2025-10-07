package com.gabrielrq.database_converter.domain;

import java.util.List;

public record TableDefinition(
        String name,
        List<ColumnDefinition> columns,
        List<String> primaryKeyColumns,
        List<ForeignKeyDefinition> foreignKeys,
        List<List<String>> uniqueConstraints,
        String schema
) {
}
