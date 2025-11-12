package com.gabrielrq.database_converter.service.etl;


import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.EtlRequestDTO;
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

    public MigrationStatus createNew(String name) {
        MigrationStatus status = new MigrationStatus();
        status.setStep(EtlStep.START);
        status.setName(name);
        status.setStartedAt(LocalDateTime.now());
        statusRepository.save(status);
        return status;
    }

    public void startExtraction(EtlRequestDTO req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.START) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' já foi iniciada.");
        }

        asyncEtlExecutorService.startExtraction(req, status);
    }

    public void startTransformation(EtlRequestDTO req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.EXTRACTION_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' possui etapa de extração pendente.");
        }

        asyncEtlExecutorService.startTransformation(req, status);
    }

    public void startLoading(EtlRequestDTO req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.WAITING_FOR_LOAD_CONFIRMATION && status.getStep() != EtlStep.TRANSFORMATION_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' possui etapa de transformação pendente.");
        }

        asyncEtlExecutorService.startLoading(req, status);
    }

    public MigrationStatus getCurrentStatus(UUID id) {
        return statusRepository.find(id);
    }
}
