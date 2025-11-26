package com.gabrielrq.database_converter.controller;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.MigrationStatusDTO;
import com.gabrielrq.database_converter.dto.SqlDTO;
import com.gabrielrq.database_converter.dto.SqlPageDTO;
import com.gabrielrq.database_converter.dto.StartMigrationRequestDTO;
import com.gabrielrq.database_converter.mapper.MigrationStatusMapper;
import com.gabrielrq.database_converter.service.SqlService;
import com.gabrielrq.database_converter.service.SseService;
import com.gabrielrq.database_converter.service.etl.EtlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/migrations")
public class MigrationController {

    private final EtlService etlService;
    private final SseService sseService;
    private final SqlService sqlService;

    public MigrationController(EtlService etlService, SseService sseService, SqlService sqlService) {
        this.etlService = etlService;
        this.sseService = sseService;
        this.sqlService = sqlService;
    }

    @PostMapping
    public ResponseEntity<MigrationStatusDTO> newMigration(@RequestBody StartMigrationRequestDTO startMigrationRequestDTO) {
        MigrationStatus status = etlService.createNew(startMigrationRequestDTO);
        return ResponseEntity.ok(MigrationStatusMapper.toMigrationStatusDTO(status));
    }

    @PostMapping("/{id}/extract")
    public ResponseEntity<Void> startExtraction(@PathVariable UUID id) {
        etlService.startExtraction(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/transform")
    public ResponseEntity<Void> startTransformation(@PathVariable UUID id) {
        etlService.startTransformation(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/load")
    public ResponseEntity<Void> startLoad(@PathVariable UUID id) {
        etlService.startLoading(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> startValidation(@PathVariable UUID id) {
        etlService.startConsistencyValidation(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<MigrationStatusDTO> getStatus(@PathVariable UUID id) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        return ResponseEntity.ok(MigrationStatusMapper.toMigrationStatusDTO(status));
    }

    @GetMapping("/{id}/sse")
    public SseEmitter getSseEvents(@PathVariable UUID id) {
        return sseService.registerEmitter(id);
    }

    @GetMapping("/{id}/sql")
    public ResponseEntity<SqlPageDTO> listSql(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        return ResponseEntity.ok(sqlService.listDDL(status.getName(), page, size));
    }

    @PutMapping("/{id}/sql")
    public ResponseEntity<Void> updateSqlFile(@PathVariable UUID id, @RequestBody List<SqlDTO> sqlFiles) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        sqlService.updateDDL(status.getName(), sqlFiles);
        return ResponseEntity.noContent().build();
    }
}
