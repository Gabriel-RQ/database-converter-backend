package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.dto.StartMigrationRequestDTO;
import com.gabrielrq.database_converter.enums.EtlStep;
import com.gabrielrq.database_converter.exception.InvalidMigrationStateException;
import com.gabrielrq.database_converter.repository.EtlStatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EtlServiceTest {

    @Mock
    private AsyncEtlExecutorService asyncEtlExecutorService;

    @Mock
    private EtlStatusRepository statusRepository;

    @InjectMocks
    private EtlService etlService;

    @Test
    @DisplayName("Deve criar uma nova migração com sucesso")
    void shouldCreateNewMigration() {
        // Arrange
        StartMigrationRequestDTO request = new StartMigrationRequestDTO(
                "Migration Test", "POSTGRES",
                new DbConnectionConfigDTO("origin", "url", "user", "pass", "driver"),
                new DbConnectionConfigDTO("target", "url", "user", "pass", "driver")
        );

        // Act
        MigrationStatus result = etlService.createNew(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStep()).isEqualTo(EtlStep.START);
        assertThat(result.getName()).isEqualTo("Migration Test");

        verify(statusRepository, times(1)).save(any(MigrationStatus.class));
    }

    @Test
    @DisplayName("Não deve iniciar extração se o status não for START")
    void shouldNotStartExtractionIfStatusIsNotStart() {
        // Arrange
        UUID id = UUID.randomUUID();
        MigrationStatus status = new MigrationStatus();
        status.setId(id);
        status.setStep(EtlStep.EXTRACTION_IN_PROGRESS);

        when(statusRepository.find(id)).thenReturn(status);

        // Act & Assert
        assertThrows(InvalidMigrationStateException.class, () -> {
            etlService.startExtraction(id);
        });

        verify(asyncEtlExecutorService, never()).startExtraction(any());
    }

    @Test
    @DisplayName("Deve iniciar extração se o status for START")
    void shouldStartExtractionIfStatusIsStart() {
        // Arrange
        UUID id = UUID.randomUUID();
        MigrationStatus status = new MigrationStatus();
        status.setId(id);
        status.setStep(EtlStep.START);

        when(statusRepository.find(id)).thenReturn(status);

        // Act
        etlService.startExtraction(id);

        // Assert
        verify(asyncEtlExecutorService, times(1)).startExtraction(status);
    }
}
