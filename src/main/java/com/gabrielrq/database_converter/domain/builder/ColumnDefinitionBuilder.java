package com.gabrielrq.database_converter.domain.builder;

import com.gabrielrq.database_converter.domain.ColumnDefinition;

public class ColumnDefinitionBuilder {
    private String name;
    private Integer genericType;
    private String originType;
    private String targetType;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean isNullable;
    private boolean isAutoIncrement;
    private String defaultValue;
    private Integer ordinalPosition;

    public static ColumnDefinitionBuilder from(ColumnDefinition column) {
        return new ColumnDefinitionBuilder()
                .setName(column.name())
                .setOriginType(column.originType())
                .setTargetType(column.targetType())
                .setLength(column.length())
                .setPrecision(column.precision())
                .setScale(column.scale())
                .setNullable(column.isNullable())
                .setAutoIncrement(column.isAutoIncrement())
                .setDefaultValue(column.defaultValue())
                .setOrdinalPosition(column.ordinalPosition())
                .setGenericType(column.genericType());
    }

    public ColumnDefinitionBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ColumnDefinitionBuilder setOriginType(String originType) {
        this.originType = originType;
        return this;
    }

    public ColumnDefinitionBuilder setTargetType(String targetType) {
        this.targetType = targetType;
        return this;
    }

    public ColumnDefinitionBuilder setLength(Integer length) {
        this.length = length;
        return this;
    }

    public ColumnDefinitionBuilder setPrecision(Integer precision) {
        this.precision = precision;
        return this;
    }

    public ColumnDefinitionBuilder setScale(Integer scale) {
        this.scale = scale;
        return this;
    }

    public ColumnDefinitionBuilder setNullable(boolean nullable) {
        this.isNullable = nullable;
        return this;
    }

    public ColumnDefinitionBuilder setAutoIncrement(boolean autoIncrement) {
        this.isAutoIncrement = autoIncrement;
        return this;
    }

    public ColumnDefinitionBuilder setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ColumnDefinitionBuilder setOrdinalPosition(Integer ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
        return this;
    }

    public ColumnDefinitionBuilder setGenericType(Integer genericType) {
        this.genericType = genericType;
        return this;
    }

    public ColumnDefinition build() {
        return new ColumnDefinition(
                name,
                genericType,
                originType,
                targetType,
                length,
                precision,
                scale,
                isNullable,
                isAutoIncrement,
                defaultValue,
                ordinalPosition
        );
    }
}
