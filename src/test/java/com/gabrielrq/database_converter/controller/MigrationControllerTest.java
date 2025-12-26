package com.gabrielrq.database_converter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.SqlDTO;
import com.gabrielrq.database_converter.dto.SqlPageDTO;
import com.gabrielrq.database_converter.dto.StartMigrationRequestDTO;
import com.gabrielrq.database_converter.enums.EtlStep;
import com.gabrielrq.database_converter.exception.InvalidMigrationStateException;
import com.gabrielrq.database_converter.exception.NonExistentMigrationException;
import com.gabrielrq.database_converter.service.SqlService;
import com.gabrielrq.database_converter.service.SseService;
import com.gabrielrq.database_converter.service.etl.EtlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MigrationController.class)
public class MigrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EtlService etlService;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private SqlService sqlService;

    @Test
    @DisplayName("POST /api/v1/migrations - Deve retornar 200 e o status inicial")
    void shouldCreateNewMigration() throws Exception {
        // Arrange
        StartMigrationRequestDTO request = new StartMigrationRequestDTO(
                "Test Migration", "FIREBIRD", null, null
        );

        MigrationStatus mockStatus = new MigrationStatus();
        mockStatus.setId(UUID.randomUUID());
        mockStatus.setName("Test Migration");
        mockStatus.setStep(EtlStep.START);

        when(etlService.createNew(any(StartMigrationRequestDTO.class))).thenReturn(mockStatus);

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Migration"))
                .andExpect(jsonPath("$.step").value("START"));
    }

    @Test
    @DisplayName("POST /{id}/extract - Deve iniciar a extração e retornar 202 Accepted")
    void shouldStartExtraction() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations/{id}/extract", id))
                .andExpect(status().isAccepted());

        verify(etlService).startExtraction(id);
    }

    @Test
    @DisplayName("POST /{id}/transform - Deve iniciar a transformação e retornar 202 Accepted")
    void shouldStartTransformation() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations/{id}/transform", id))
                .andExpect(status().isAccepted());

        verify(etlService).startTransformation(id);
    }

    @Test
    @DisplayName("POST /{id}/load - Deve iniciar a carga e retornar 202 Accepted")
    void shouldStartLoad() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations/{id}/load", id))
                .andExpect(status().isAccepted());

        verify(etlService).startLoading(id);
    }

    @Test
    @DisplayName("POST /{id}/validate - Deve iniciar a validação de consistência e retornar 202 Accepted")
    void shouldStartValidation() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations/{id}/validate", id))
                .andExpect(status().isAccepted());

        verify(etlService).startConsistencyValidation(id);
    }

    @Test
    @DisplayName("GET /{id}/status - Deve retornar o status atual da migração")
    void shouldGetStatus() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        MigrationStatus mockStatus = new MigrationStatus();
        mockStatus.setId(id);
        mockStatus.setName("Migration Check");
        mockStatus.setStep(EtlStep.EXTRACTION_FINISHED);

        when(etlService.getCurrentStatus(id)).thenReturn(mockStatus);

        // Act & Assert
        mockMvc.perform(get("/api/v1/migrations/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.step").value("EXTRACTION_FINISHED"));
    }

    @Test
    @DisplayName("GET /{id}/sse - Deve registrar e retornar um SseEmitter")
    void shouldGetSseEvents() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        SseEmitter mockEmitter = new SseEmitter();

        when(sseService.registerEmitter(id)).thenReturn(mockEmitter);

        // Act & Assert
        mockMvc.perform(get("/api/v1/migrations/{id}/sse", id))
                .andExpect(status().isOk());

        verify(sseService).registerEmitter(id);
    }

    @Test
    @DisplayName("GET /{id}/sql - Deve listar arquivos SQL paginados")
    void shouldListSqlFiles() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String migrationName = "migration-test";

        MigrationStatus mockStatus = new MigrationStatus();
        mockStatus.setName(migrationName);
        when(etlService.getCurrentStatus(id)).thenReturn(mockStatus);

        List<SqlDTO> files = List.of(new SqlDTO("script.sql", "SELECT 1"));
        SqlPageDTO mockPage = new SqlPageDTO(0, 10, 1, files);

        when(sqlService.listDDL(eq(migrationName), anyInt(), anyInt())).thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/migrations/{id}/sql", id)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.files[0].filename").value("script.sql"));
    }

    @Test
    @DisplayName("PUT /{id}/sql - Deve atualizar arquivos SQL e retornar 204 No Content")
    void shouldUpdateSqlFiles() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String migrationName = "migration-test";

        MigrationStatus mockStatus = new MigrationStatus();
        mockStatus.setName(migrationName);
        when(etlService.getCurrentStatus(id)).thenReturn(mockStatus);

        List<SqlDTO> sqlFiles = List.of(new SqlDTO("script.sql", "UPDATE..."));

        // Act & Assert
        mockMvc.perform(put("/api/v1/migrations/{id}/sql", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sqlFiles)))
                .andExpect(status().isNoContent());

        verify(sqlService).updateDDL(eq(migrationName), anyList());
    }

    @Test
    @DisplayName("POST /{id}/extract - Deve retornar 400 Bad Request se a migração não existir")
    void shouldReturnBadRequestWhenMigrationNotFound() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        doThrow(new NonExistentMigrationException("Migração não encontrada"))
                .when(etlService).startExtraction(id);

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations/{id}/extract", id))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.message").value("Migração não encontrada"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /{id}/transform - Deve retornar 422 Unprocessable Entity se o estado for inválido")
    void shouldReturnUnprocessableEntityWhenInvalidState() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();

        doThrow(new InvalidMigrationStateException("Estado inválido"))
                .when(etlService).startTransformation(id);

        // Act & Assert
        mockMvc.perform(post("/api/v1/migrations/{id}/transform", id))
                .andExpect(status().isUnprocessableEntity()) // 422
                .andExpect(jsonPath("$.message").value("Estado inválido"))
                .andExpect(jsonPath("$.status").value(422));
    }
}
