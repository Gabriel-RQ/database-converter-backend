package com.gabrielrq.database_converter.service.etl;


import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.domain.MigrationStatusMetadata;
import com.gabrielrq.database_converter.domain.builder.MigrationStatusMetadataBuilder;
import com.gabrielrq.database_converter.dto.StartMigrationRequestDTO;
import com.gabrielrq.database_converter.enums.EtlStep;
import com.gabrielrq.database_converter.exception.InvalidMigrationStateException;
import com.gabrielrq.database_converter.repository.EtlStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EtlService {

    private final AsyncEtlExecutorService asyncEtlExecutorService;
    private final EtlStatusRepository statusRepository;

    public EtlService(AsyncEtlExecutorService asyncEtlExecutor, EtlStatusRepository statusRepository) {
        this.asyncEtlExecutorService = asyncEtlExecutor;
        this.statusRepository = statusRepository;
    }

    public MigrationStatus createNew(StartMigrationRequestDTO startMigrationRequestDTO) {
        MigrationStatus status = new MigrationStatus();
        MigrationStatusMetadata statusMetadata = new MigrationStatusMetadataBuilder()
                .setTarget(startMigrationRequestDTO.target())
                .setOriginConfig(startMigrationRequestDTO.originConfig())
                .setTargetConfig(startMigrationRequestDTO.targetConfig())
                .build();

        status.setName(startMigrationRequestDTO.name());
        status.setMetadata(statusMetadata);
        status.setStep(EtlStep.START);
        status.setStartedAt(LocalDateTime.now());
        statusRepository.save(status);
        return status;
    }

    public void startExtraction(UUID id) {
        MigrationStatus status = statusRepository.find(id);

        if (status.getStep() != EtlStep.START) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' já foi iniciada.");
        }

        asyncEtlExecutorService.startExtraction(status);
    }

    public void startTransformation(UUID id) {
        MigrationStatus status = statusRepository.find(id);

        if (status.getStep() != EtlStep.EXTRACTION_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não pode iniciar transformação, pois não possui extração finalizada.");
        }

        asyncEtlExecutorService.startTransformation(status);
    }

    public void startLoading(UUID id) {
        MigrationStatus status = statusRepository.find(id);

        if (status.getStep() != EtlStep.WAITING_FOR_LOAD_CONFIRMATION && status.getStep() != EtlStep.TRANSFORMATION_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não possui etapa de transformação finalizada, nem está aguardando para confirmação de carga.");
        }

        asyncEtlExecutorService.startLoading(status);
    }

    public void startConsistencyValidation(UUID id) {
        MigrationStatus status = statusRepository.find(id);

        if (status.getStep() != EtlStep.LOAD_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não pode ser validada, pois possui etapa de carga pendente.");
        }

        asyncEtlExecutorService.startConsistencyValidation(status);
    }

    public MigrationStatus getCurrentStatus(UUID id) {
        return statusRepository.find(id);
    }
}
