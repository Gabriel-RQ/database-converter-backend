package com.gabrielrq.database_converter.domain.builder;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;

import java.util.ArrayList;
import java.util.List;

public class DatabaseDefinitionBuilder {
    private String name;
    private String schema;
    private List<TableDefinition> tables = new ArrayList<>();
    private String characterSet;

    public DatabaseDefinitionBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public DatabaseDefinitionBuilder setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public DatabaseDefinitionBuilder setTables(List<TableDefinition> tables) {
        this.tables = tables;
        return this;
    }

    public DatabaseDefinitionBuilder setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
        return this;
    }

    public DatabaseDefinitionBuilder addTable(TableDefinition table) {
        this.tables.add(table);
        return this;
    }

    public static DatabaseDefinitionBuilder from(DatabaseDefinition database) {
        return new DatabaseDefinitionBuilder()
                .setName(database.name())
                .setSchema(database.schema())
                .setTables(database.tables())
                .setCharacterSet(database.characterSet());
    }

    public DatabaseDefinition build() {
        return new DatabaseDefinition(name, schema, tables, characterSet);
    }
}
