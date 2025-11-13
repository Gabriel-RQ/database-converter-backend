package com.gabrielrq.database_converter.domain;

public record ColumnDefinition(
        String name,
        Integer genericType,
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

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ColumnDefinition that)) return false;

        return name.equalsIgnoreCase(that.name) && genericType.equals(that.genericType);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + genericType.hashCode();
        return result;
    }
}
