package com.gabrielrq.database_converter.controller;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.*;
import com.gabrielrq.database_converter.mapper.MigrationStatusMapper;
import com.gabrielrq.database_converter.service.SqlService;
import com.gabrielrq.database_converter.service.SseService;
import com.gabrielrq.database_converter.service.etl.EtlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/migrations")
@Tag(name = "Migrações", description = "Endpoints para gerenciamento do processo de migração de dados")
public class MigrationController {

    private final EtlService etlService;
    private final SseService sseService;
    private final SqlService sqlService;

    public MigrationController(EtlService etlService, SseService sseService, SqlService sqlService) {
        this.etlService = etlService;
        this.sseService = sseService;
        this.sqlService = sqlService;
    }

    @Operation(summary = "Inicia uma nova migração", description = "Cria um novo registro de migração com as configurações de origem e destino.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Migração criada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<MigrationStatusDTO> newMigration(@RequestBody StartMigrationRequestDTO startMigrationRequestDTO) {
        MigrationStatus status = etlService.createNew(startMigrationRequestDTO);
        return ResponseEntity.ok(MigrationStatusMapper.toMigrationStatusDTO(status));
    }

    @Operation(summary = "Inicia a extração", description = "Inicia o processo de leitura dos metadados e dados do banco de origem.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Processo de extração iniciado"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Estado inválido para extração (ex: já iniciada)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/extract")
    public ResponseEntity<Void> startExtraction(@PathVariable UUID id) {
        etlService.startExtraction(id);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Inicia a transformação", description = "Converte os tipos de dados e gera os scripts DDL/DML para o banco de destino.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Processo de transformação iniciado"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Estado inválido para transformação (ex: extração pendente)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/transform")
    public ResponseEntity<Void> startTransformation(@PathVariable UUID id) {
        etlService.startTransformation(id);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Inicia a carga", description = "Executa os scripts gerados no banco de dados de destino.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Processo de carga iniciado"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Estado inválido para carga (ex: aguardando confirmação)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/load")
    public ResponseEntity<Void> startLoad(@PathVariable UUID id) {
        etlService.startLoading(id);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Inicia a validação", description = "Compara a estrutura e volumetria entre origem e destino para garantir integridade.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Processo de validação iniciado"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Estado inválido para validação (ex: carga não finalizada)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> startValidation(@PathVariable UUID id) {
        etlService.startConsistencyValidation(id);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Obter status", description = "Retorna o estado atual e metadados da migração.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/status")
    public ResponseEntity<MigrationStatusDTO> getStatus(@PathVariable UUID id) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        return ResponseEntity.ok(MigrationStatusMapper.toMigrationStatusDTO(status));
    }

    @Operation(summary = "Assinar eventos SSE", description = "Abre uma conexão Server-Sent Events para receber atualizações de progresso em tempo real.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conexão SSE estabelecida"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/sse")
    public SseEmitter getSseEvents(@PathVariable UUID id) {
        return sseService.registerEmitter(id);
    }

    @Operation(summary = "Listar arquivos SQL", description = "Lista os arquivos DDL gerados de forma paginada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de arquivos retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/sql")
    public ResponseEntity<SqlPageDTO> listSql(
            @PathVariable UUID id,
            @Parameter(description = "Número da página (inicia em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "5") int size
    ) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        return ResponseEntity.ok(sqlService.listDDL(status.getName(), page, size));
    }

    @Operation(summary = "Atualizar arquivo SQL", description = "Permite editar manualmente o conteúdo de um script SQL gerado antes da carga.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Arquivo atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Migração não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}/sql")
    public ResponseEntity<Void> updateSqlFile(@PathVariable UUID id, @RequestBody List<SqlDTO> sqlFiles) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        sqlService.updateDDL(status.getName(), sqlFiles);
        return ResponseEntity.noContent().build();
    }
}
