package com.gabrielrq.database_converter.service.etl;


import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.EtlRequest;
import com.gabrielrq.database_converter.dto.TransformationResult;
import com.gabrielrq.database_converter.enums.EtlStep;
import com.gabrielrq.database_converter.repository.EtlStatusRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EtlService {
    private final DataExtractionService extractionService;
    private final DataTransformationService transformationService;
    private final DataLoadingService loadingService;
    private final EtlStatusRepository statusRepository;

    public EtlService(DataExtractionService extractionService, DataTransformationService transformationService, DataLoadingService loadingService, EtlStatusRepository statusRepository) {
        this.extractionService = extractionService;
        this.transformationService = transformationService;
        this.loadingService = loadingService;
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

    @Async
    public void startExtraction(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());
        status.setStep(EtlStep.EXTRACTION_IN_PROGRESS);
        statusRepository.save(status);

        try {
            DatabaseDefinition metadata = extractionService.extract(req.config());
            status.setMetadata(metadata);
            status.setStep(EtlStep.EXTRACTION_FINISHED);
            statusRepository.save(status);
        } catch (Exception e) {
            status.setStep(EtlStep.ERROR);
            status.setMessage(e.getMessage());
            statusRepository.save(status);
        }
    }

    @Async
    public void startTransformation(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());
        status.setStep(EtlStep.TRANSFORMATION_IN_PROGRESS);
        statusRepository.save(status);

        try {
            TransformationResult result = transformationService.transform(status.getMetadata(), req.target());
            status.setMetadata(result.metadata());
            status.setExecutionOrder(result.executionList());
            status.setStep(EtlStep.TRANSFORMATION_FINISHED);
            statusRepository.save(status);
            status.setStep(EtlStep.WAITING_FOR_LOAD_CONFIRMATION);
            statusRepository.save(status);
        } catch (Exception e) {
            status.setStep(EtlStep.ERROR);
            status.setMessage(e.getMessage());
            statusRepository.save(status);
        }
    }


    @Async
    public void startLoad(EtlRequest req) {
        MigrationStatus status = statusRepository.find(req.id());
        status.setStep(EtlStep.LOAD_IN_PROGRESS);
        statusRepository.save(status);

        try {
            loadingService.load(req.config(), new TransformationResult(status.getMetadata(), status.getExecutionOrder()));
            status.setStep(EtlStep.FINISHED);
            status.setFinishedAt(LocalDateTime.now());
            statusRepository.save(status);
        } catch (Exception e) {
            status.setStep(EtlStep.ERROR);
            status.setMessage(e.getMessage());
            statusRepository.save(status);
        }
    }

    public MigrationStatus getCurrentStatus(UUID id) {
        return statusRepository.find(id);
    }
}
