package com.gabrielrq.database_converter.domain;

import java.util.List;

public record ForeignKeyDefinition(
        String name,
        String referencedTable,
        List<String> localColumns,
        List<String> referencedColumns
) {
}
