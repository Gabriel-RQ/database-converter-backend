package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.domain.MigrationStatusMetadata;
import com.gabrielrq.database_converter.domain.TransformationResult;
import com.gabrielrq.database_converter.dto.ConsistencyValidationDataDTO;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.enums.EtlStep;
import com.gabrielrq.database_converter.repository.EtlStatusRepository;
import com.gabrielrq.database_converter.service.ConsistencyValidationService;
import com.gabrielrq.database_converter.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncEtlExecutorServiceTest {

    @Mock private DataExtractionService extractionService;
    @Mock private DataTransformationService transformationService;
    @Mock private DataLoadingService loadingService;
    @Mock private ConsistencyValidationService validationService;
    @Mock private EtlStatusRepository statusRepository;
    @Mock private SseService sseService;

    @InjectMocks
    private AsyncEtlExecutorService asyncExecutor;

    private MigrationStatus status;
    private MigrationStatusMetadata metadata;
    private List<EtlStep> savedSteps;

    @BeforeEach
    void setUp() {
        status = new MigrationStatus();
        status.setId(UUID.randomUUID());

        metadata = new MigrationStatusMetadata();
        metadata.setOriginConfig(mock(DbConnectionConfigDTO.class));
        metadata.setTargetConfig(mock(DbConnectionConfigDTO.class));
        metadata.setTarget("POSTGRES");

        status.setMetadata(metadata);

        savedSteps = new ArrayList<>();

        lenient().doAnswer(invocation -> {
            MigrationStatus arg = invocation.getArgument(0);
            savedSteps.add(arg.getStep());
            return null;
        }).when(statusRepository).save(any(MigrationStatus.class));
    }

    // --- EXTRACTION TESTS ---

    @Test
    @DisplayName("Extração: Deve executar com sucesso e atualizar status sequencialmente")
    void shouldExecuteExtractionSuccessfully() {
        // Arrange
        DatabaseDefinition mockDbDef = mock(DatabaseDefinition.class);
        when(extractionService.extract(any(), any())).thenReturn(mockDbDef);

        // Act
        asyncExecutor.startExtraction(status);

        // Assert
        verify(extractionService).extract(eq(status.getId().toString()), eq(metadata.getOriginConfig()));

        assertThat(savedSteps).containsExactly(
                EtlStep.EXTRACTION_IN_PROGRESS,
                EtlStep.EXTRACTION_FINISHED
        );
        assertThat(status.getMetadata().getDatabaseMetadata()).isEqualTo(mockDbDef);
        verify(sseService, atLeast(2)).sendMigrationStatusUpdate(status);
    }

    @Test
    @DisplayName("Extração: Deve capturar erro e atualizar status para ERROR")
    void shouldHandleExtractionError() {
        // Arrange
        doThrow(new RuntimeException("Falha na conexão")).when(extractionService).extract(any(), any());

        // Act
        asyncExecutor.startExtraction(status);

        // Assert
        assertThat(savedSteps).isNotEmpty();
        assertThat(savedSteps.getLast()).isEqualTo(EtlStep.ERROR);
        assertThat(status.getMessage()).isEqualTo("Falha na conexão");
        verify(sseService, atLeastOnce()).sendMigrationStatusUpdate(status);
    }

    // --- TRANSFORMATION TESTS ---

    @Test
    @DisplayName("Transformação: Deve executar com sucesso e aguardar confirmação de carga")
    void shouldExecuteTransformationSuccessfully() {
        // Arrange
        metadata.setDatabaseMetadata(mock(DatabaseDefinition.class));
        TransformationResult result = new TransformationResult(mock(DatabaseDefinition.class), Collections.emptyList());
        when(transformationService.transform(any(), any(), any())).thenReturn(result);

        // Act
        asyncExecutor.startTransformation(status);

        // Assert
        verify(transformationService).transform(eq(status.getId().toString()), any(), eq("POSTGRES"));
        assertThat(savedSteps).containsExactly(
                EtlStep.TRANSFORMATION_IN_PROGRESS,
                EtlStep.TRANSFORMATION_FINISHED,
                EtlStep.WAITING_FOR_LOAD_CONFIRMATION
        );
    }

    @Test
    @DisplayName("Transformação: Deve tratar erro")
    void shouldHandleTransformationError() {
        // Arrange
        metadata.setDatabaseMetadata(mock(DatabaseDefinition.class));
        doThrow(new RuntimeException("Erro de mapeamento")).when(transformationService).transform(any(), any(), any());

        // Act
        asyncExecutor.startTransformation(status);

        // Assert
        assertThat(savedSteps.getLast()).isEqualTo(EtlStep.ERROR);
        assertThat(status.getMessage()).isEqualTo("Erro de mapeamento");
    }

    // --- LOADING TESTS ---

    @Test
    @DisplayName("Carga: Deve executar com sucesso")
    void shouldExecuteLoadingSuccessfully() {
        // Arrange
        metadata.setDatabaseMetadata(mock(DatabaseDefinition.class));
        metadata.setExecutionOrder(Collections.emptyList());

        // Act
        asyncExecutor.startLoading(status);

        // Assert
        verify(loadingService).load(eq(status.getId().toString()), eq(metadata.getTargetConfig()), any(TransformationResult.class));
        assertThat(savedSteps).containsExactly(
                EtlStep.LOAD_IN_PROGRESS,
                EtlStep.LOAD_FINISHED
        );
    }

    @Test
    @DisplayName("Carga: Deve tratar erro")
    void shouldHandleLoadingError() {
        // Arrange
        doThrow(new RuntimeException("SQL Error")).when(loadingService).load(any(), any(), any());

        // Act
        asyncExecutor.startLoading(status);

        // Assert
        assertThat(savedSteps.getLast()).isEqualTo(EtlStep.ERROR);
    }

    // --- VALIDATION TESTS ---

    @Test
    @DisplayName("Validação: Deve finalizar migração e fechar SSE")
    void shouldExecuteValidationSuccessfully() {
        // Arrange
        ConsistencyValidationDataDTO validationData = new ConsistencyValidationDataDTO(true, true, true, List.of("OK"));
        when(validationService.validate(any(), any(), any())).thenReturn(validationData);

        // Act
        asyncExecutor.startConsistencyValidation(status);

        // Assert
        verify(validationService).validate(eq(status.getId().toString()), eq(metadata.getOriginConfig()), eq(metadata.getTargetConfig()));
        assertThat(savedSteps).containsExactly(
                EtlStep.VALIDATION_IN_PROGRESS,
                EtlStep.FINISHED
        );
        assertThat(status.getMessage()).contains("OK");
        verify(sseService).completeSseEmitter(status.getId());
    }

    @Test
    @DisplayName("Validação: Deve tratar erro e fechar SSE mesmo assim")
    void shouldHandleValidationError() {
        // Arrange
        doThrow(new RuntimeException("Erro de comparação")).when(validationService).validate(any(), any(), any());

        // Act
        asyncExecutor.startConsistencyValidation(status);

        // Assert
        assertThat(savedSteps.getLast()).isEqualTo(EtlStep.ERROR);
        verify(sseService).completeSseEmitter(status.getId());
    }
}