package com.gabrielrq.database_converter.domain;

import java.util.List;

public record TransformationResult(
        DatabaseDefinition metadata,
        List<TableDefinition> executionList
) {
}
