package com.gabrielrq.database_converter.domain;

import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;

import java.util.List;

public class MigrationStatusMetadata {
    private String target;
    private DatabaseDefinition databaseMetadata;
    private List<TableDefinition> executionOrder;
    private DbConnectionConfigDTO originConfig;
    private DbConnectionConfigDTO targetConfig;

    public MigrationStatusMetadata(String target, DatabaseDefinition databaseMetadata, List<TableDefinition> executionOrder, DbConnectionConfigDTO originConfig, DbConnectionConfigDTO targetConfig) {
        this.target = target;
        this.databaseMetadata = databaseMetadata;
        this.executionOrder = executionOrder;
        this.originConfig = originConfig;
        this.targetConfig = targetConfig;
    }

    public MigrationStatusMetadata() {
    }


    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public DatabaseDefinition getDatabaseMetadata() {
        return databaseMetadata;
    }

    public void setDatabaseMetadata(DatabaseDefinition databaseMetadata) {
        this.databaseMetadata = databaseMetadata;
    }

    public List<TableDefinition> getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(List<TableDefinition> executionOrder) {
        this.executionOrder = executionOrder;
    }

    public DbConnectionConfigDTO getOriginConfig() {
        return originConfig;
    }

    public void setOriginConfig(DbConnectionConfigDTO originConfig) {
        this.originConfig = originConfig;
    }

    public DbConnectionConfigDTO getTargetConfig() {
        return targetConfig;
    }

    public void setTargetConfig(DbConnectionConfigDTO targetConfig) {
        this.targetConfig = targetConfig;
    }
}
