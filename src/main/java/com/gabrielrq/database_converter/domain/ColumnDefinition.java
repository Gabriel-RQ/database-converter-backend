package com.gabrielrq.database_converter.domain;

public record ColumnDefinition(
        String name,
        String originType,
        String targetType,
        Integer length,
        Integer precision,
        Integer scale,
        boolean isNullable,
        boolean isAutoIncrement,
        String defaultValue,
        Integer ordinalPosition
) {
}
