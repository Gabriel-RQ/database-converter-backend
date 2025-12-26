package com.gabrielrq.database_converter.repository;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.exception.NonExistentMigrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EtlStatusRepositoryTest {

    private EtlStatusRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EtlStatusRepository();
    }

    @Test
    @DisplayName("Deve salvar e recuperar um status de migração")
    void shouldSaveAndFind() {
        // Arrange
        UUID id = UUID.randomUUID();
        MigrationStatus status = new MigrationStatus();
        status.setId(id);
        status.setName("Test");

        // Act
        repository.save(status);
        MigrationStatus found = repository.find(id);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test");
        assertThat(found.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar ID inexistente")
    void shouldThrowExceptionWhenNotFound() {
        // Act & Assert
        assertThrows(NonExistentMigrationException.class, () -> {
            repository.find(UUID.randomUUID());
        });
    }
}