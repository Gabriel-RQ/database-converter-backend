package com.gabrielrq.database_converter.controller;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.dto.etl.*;
import com.gabrielrq.database_converter.dto.MigrationStatusDTO;
import com.gabrielrq.database_converter.dto.StartMigrationRequestDTO;
import com.gabrielrq.database_converter.mapper.EtlRequestMapper;
import com.gabrielrq.database_converter.mapper.MigrationStatusMapper;
import com.gabrielrq.database_converter.service.SseService;
import com.gabrielrq.database_converter.service.etl.EtlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

    private final EtlService etlService;
    private final SseService sseService;

    public MigrationController(EtlService etlService, SseService sseService) {
        this.etlService = etlService;
        this.sseService = sseService;
    }

    @PostMapping("/new")
    public ResponseEntity<MigrationStatusDTO> newMigration(@RequestBody StartMigrationRequestDTO startMigrationRequestDTO) {
        MigrationStatus status = etlService.createNew(startMigrationRequestDTO.name());
        return ResponseEntity.ok(MigrationStatusMapper.toMigrationStatusDTO(status));
    }

    @PostMapping("/extract")
    public ResponseEntity<Void> startExtraction(@RequestBody ExtractionRequestDTO extractionRequestDTO) {
        etlService.startExtraction(EtlRequestMapper.fromExtractionRequestDTO(extractionRequestDTO));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/transform")
    public ResponseEntity<Void> startTransformation(@RequestBody TransformationRequestDTO transformationRequestDTO) {
        etlService.startTransformation(EtlRequestMapper.fromTransformationRequestDTO(transformationRequestDTO));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/load")
    public ResponseEntity<Void> startLoad(@RequestBody LoadRequestDTO loadRequestDTO) {
        etlService.startLoading(EtlRequestMapper.fromLoadRequestDTO(loadRequestDTO));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> startValidation(@RequestBody ValidationRequestDTO validationRequestDTO) {
        etlService.startConsistencyValidation(EtlRequestMapper.fromValidationRequestDTO(validationRequestDTO));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<MigrationStatusDTO> getStatus(@PathVariable UUID id) {
        MigrationStatus status = etlService.getCurrentStatus(id);
        return ResponseEntity.ok(MigrationStatusMapper.toMigrationStatusDTO(status));
    }

    @GetMapping("/{id}/sse")
    public SseEmitter getSseEvents(@PathVariable UUID id) {
        return sseService.registerEmitter(id);
    }
}
