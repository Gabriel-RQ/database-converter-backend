package com.gabrielrq.database_converter.service.etl;


import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.domain.EtlRequest;
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

    public void startExtraction(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.START) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' já foi iniciada.");
        }

        asyncEtlExecutorService.startExtraction(req, status);
    }

    public void startTransformation(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.EXTRACTION_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não pode iniciar transformação, pois não possui extração finalizada.");
        }

        asyncEtlExecutorService.startTransformation(req, status);
    }

    public void startLoading(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.WAITING_FOR_LOAD_CONFIRMATION && status.getStep() != EtlStep.TRANSFORMATION_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não possui etapa de transformação finalizada, nem está aguardando para confirmação de carga.");
        }

        asyncEtlExecutorService.startLoading(req, status);
    }

    public void startConsistencyValidation(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());

        if (status.getStep() != EtlStep.LOAD_FINISHED) {
            throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não pode ser validada, pois possui etapa de carga pendente.");
        }

        asyncEtlExecutorService.startConsistencyValidation(req, status);
    }

    public MigrationStatus getCurrentStatus(UUID id) {
        return statusRepository.find(id);
    }
}
