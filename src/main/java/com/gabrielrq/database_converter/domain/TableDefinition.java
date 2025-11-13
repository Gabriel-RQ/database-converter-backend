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

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TableDefinition that)) return false;

        return name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
