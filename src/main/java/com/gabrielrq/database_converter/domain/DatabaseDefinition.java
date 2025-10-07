package com.gabrielrq.database_converter.domain;

import java.util.List;

public record DatabaseDefinition(
        String name,
        String schema,
        List<TableDefinition> tables,
        String characterSet
) {
}
