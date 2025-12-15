package com.gabrielrq.database_converter.repository;


import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.exception.NonExistentMigrationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EtlStatusRepository {

    private final Map<UUID, MigrationStatus> repo = new ConcurrentHashMap<>();

    public void save(MigrationStatus status) {
        status.setLastUpdated(LocalDateTime.now());
        repo.put(status.getId(), status);
    }

    public MigrationStatus find(UUID id) {
        MigrationStatus status = repo.get(id);
        if (status == null) {
            throw new NonExistentMigrationException(
                    "Migração com ID '" + id + "' não encontrada."
            );
        }
        return status;
    }
}
