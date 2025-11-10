package com.gabrielrq.database_converter.domain;

import com.gabrielrq.database_converter.enums.EtlStep;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MigrationStatus {
    private UUID id;
    private EtlStep step;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime lastUpdated;
    private DatabaseDefinition metadata;
    private List<TableDefinition> executionOrder;
    private String name;

    public MigrationStatus() {
        id = UUID.randomUUID();
    }

    public MigrationStatus(UUID id, EtlStep step) {
        this.id = id;
        this.step = step;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EtlStep getStep() {
        return step;
    }

    public void setStep(EtlStep step) {
        this.step = step;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public DatabaseDefinition getMetadata() {
        return metadata;
    }

    public void setMetadata(DatabaseDefinition metadata) {
        this.metadata = metadata;
    }

    public List<TableDefinition> getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(List<TableDefinition> executionOrder) {
        this.executionOrder = executionOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
