package com.gabrielrq.database_converter.domain.builder;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.MigrationStatusMetadata;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;

import java.util.List;

public class MigrationStatusMetadataBuilder {
    private String target;
    private DatabaseDefinition databaseMetadata;
    private List<TableDefinition> executionOrder;
    private DbConnectionConfigDTO originConfig;
    private DbConnectionConfigDTO targetConfig;


    public MigrationStatusMetadataBuilder setTarget(String target) {
        this.target = target;
        return this;
    }

    public MigrationStatusMetadataBuilder setDatabaseMetadata(DatabaseDefinition databaseMetadata) {
        this.databaseMetadata = databaseMetadata;
        return this;
    }

    public MigrationStatusMetadataBuilder setExecutionOrder(List<TableDefinition> executionOrder) {
        this.executionOrder = executionOrder;
        return this;
    }

    public MigrationStatusMetadataBuilder setOriginConfig(DbConnectionConfigDTO originConfig) {
        this.originConfig = originConfig;
        return this;
    }

    public MigrationStatusMetadataBuilder setTargetConfig(DbConnectionConfigDTO targetConfig) {
        this.targetConfig = targetConfig;
        return this;
    }

    public MigrationStatusMetadata build() {
        return new MigrationStatusMetadata(target, databaseMetadata, executionOrder, originConfig, targetConfig);
    }
}
