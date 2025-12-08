package com.gabrielrq.database_converter.service.etl;


import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.domain.MigrationStatusMetadata;
import com.gabrielrq.database_converter.domain.builder.MigrationStatusMetadataBuilder;
import com.gabrielrq.database_converter.dto.StartMigrationRequestDTO;
import com.gabrielrq.database_converter.enums.EtlStep;
import com.gabrielrq.database_converter.exception.InvalidMigrationStateException;
import com.gabrielrq.database_converter.repository.EtlStatusRepository;
import com.gabrielrq.database_converter.util.migration.MigrationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EtlService {

    private static final Logger logger = LoggerFactory.getLogger(EtlService.class);

    private final AsyncEtlExecutorService asyncEtlExecutorService;
    private final EtlStatusRepository statusRepository;

    public EtlService(AsyncEtlExecutorService asyncEtlExecutor, EtlStatusRepository statusRepository) {
        this.asyncEtlExecutorService = asyncEtlExecutor;
        this.statusRepository = statusRepository;
    }

    public MigrationStatus createNew(StartMigrationRequestDTO startMigrationRequestDTO) {
        MigrationStatus status = new MigrationStatus();
        return MigrationLogger.withMigration(status.getId(), () -> {
            logger.info("Migração iniciada");
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
        });
    }

    public void startExtraction(UUID id) {
        MigrationLogger.withMigration(id, () -> {
            MigrationStatus status = statusRepository.find(id);

            if (status.getStep() != EtlStep.START) {
                logger.error("Processo de extração já foi iniciado");
                throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' já foi iniciada.");
            }

            logger.info("Iniciando extração");
            asyncEtlExecutorService.startExtraction(status);
        });
    }

    public void startTransformation(UUID id) {
        MigrationLogger.withMigration(id, () -> {
            MigrationStatus status = statusRepository.find(id);

            if (status.getStep() != EtlStep.EXTRACTION_FINISHED) {
                logger.error("Incapaz de iniciar transformação, possui extração pendente ou transformação já finalizada");
                throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não pode iniciar transformação pois possui extração pendente, ou possui transformação já finalizada.");
            }

            logger.info("Iniciando transformação");
            asyncEtlExecutorService.startTransformation(status);
        });
    }

    public void startLoading(UUID id) {
        MigrationLogger.withMigration(id, () -> {
            MigrationStatus status = statusRepository.find(id);

            if (status.getStep() != EtlStep.WAITING_FOR_LOAD_CONFIRMATION && status.getStep() != EtlStep.TRANSFORMATION_FINISHED) {
                logger.error("Incapaz de iniciar carga, não está aguardando confirmação de carga, nem possui transformação pendente ");
                throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não possui etapa de transformação finalizada, nem está aguardando para confirmação de carga, ou a extração já foi finalizada.");
            }

            logger.info("Iniciando carga");
            asyncEtlExecutorService.startLoading(status);
        });
    }

    public void startConsistencyValidation(UUID id) {
        MigrationLogger.withMigration(id, () -> {
            MigrationStatus status = statusRepository.find(id);

            if (status.getStep() != EtlStep.LOAD_FINISHED) {
                logger.error("Incapaz de iniciar validação de consistência, possui carga pendente ou validação já finalizada");
                throw new InvalidMigrationStateException("Migração com ID '" + status.getId() + "' não pode ser validada, pois possui etapa de carga pendente, ou a validação já foi finalizada.");
            }

            logger.info("Iniciando validação de consistência");
            asyncEtlExecutorService.startConsistencyValidation(status);
        });
    }

    public MigrationStatus getCurrentStatus(UUID id) {
        return statusRepository.find(id);
    }
}
