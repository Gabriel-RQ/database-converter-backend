package com.gabrielrq.database_converter.domain;

import com.gabrielrq.database_converter.enums.EtlStep;

import java.time.LocalDateTime;
import java.util.UUID;

public class MigrationStatus {
    private UUID id;
    private String name;
    private EtlStep step;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime lastUpdated;
    private MigrationStatusMetadata metadata;

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

    public String getName() {
        return name;
    }

    public MigrationStatus setName(String name) {
        this.name = name;
        return this;
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

    public MigrationStatusMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MigrationStatusMetadata metadata) {
        this.metadata = metadata;
    }
}
