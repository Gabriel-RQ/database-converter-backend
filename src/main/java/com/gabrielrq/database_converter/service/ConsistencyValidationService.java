package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.dto.ConsistencyValidationDataDTO;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.exception.ConsistencyValidationException;
import com.gabrielrq.database_converter.service.etl.DataExtractionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConsistencyValidationService {

    private final DataExtractionService extractionService;

    public ConsistencyValidationService(DataExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    private List<String> compareStructure(DatabaseDefinition originMetadata, DatabaseDefinition targetMetadata) {
        List<String> messages = new ArrayList<>();
        Map<String, TableDefinition> targetTableMap = targetMetadata.tables()
                .stream().collect(Collectors.toMap(t -> t.name().toLowerCase(), Function.identity()));

        for (var table : originMetadata.tables()) {
            if (!targetMetadata.tables().contains(table)) {
                messages.add("Falha estrutural: tabela '%s' não encontrada na base de dados destino.".formatted(table.name()));
                continue;
            }

            var targetTable = targetTableMap.get(table.name());
            for (var column : table.columns()) {
                if (!targetTable.columns().contains(column)) {
                    messages.add("Falha estrutural: coluna '%s.%s' não encontrada na base de dados destino.".formatted(table.name(), column.name()));
                }
            }
        }

        return messages;
    }

    private List<String> compareRowCounts(
            DatabaseDefinition originMetadata,
            DatabaseDefinition targetMetadata,
            JdbcTemplate originTemplate,
            JdbcTemplate targetTemplate
    ) {
        List<String> messages = new ArrayList<>();
        Map<String, TableDefinition> targetTableMap = targetMetadata.tables()
                .stream().collect(Collectors.toMap(t -> t.name().toLowerCase(), Function.identity()));

        for (var table : originMetadata.tables()) {
            if (!targetMetadata.tables().contains(table)) {
                continue;
            }

            try {
                var targetTable = targetTableMap.get(table.name());
                Long originRowCount = originTemplate.queryForObject("SELECT COUNT(*) FROM " + table.name(), Long.class);
                Long targetRowCount = targetTemplate.queryForObject("SELECT COUNT(*) FROM " + targetTable.name(), Long.class);

                if (!Objects.equals(originRowCount, targetRowCount)) {
                    messages.add("Falha volumétrica: quantia de linhas inconsistente para tabela '%s'. Esperava %d, encontrou %d.".formatted(table.name(), originRowCount, targetRowCount));
                }
            } catch (Exception e) {
                messages.add("Falha volumétrica: erro ao obter quantia de linhas para a tabela '%s'.".formatted(table.name()));
            }
        }

        return messages;
    }


    public ConsistencyValidationDataDTO validate(DbConnectionConfigDTO originConfig, DbConnectionConfigDTO targetConfig) {
        try (
                Connection originConnection = DatabaseConnectionService.createConnection(originConfig);
                Connection targetConnection = DatabaseConnectionService.createConnection(targetConfig)
        ) {
            var originMetadata = extractionService.parseMetadata(originConfig.name(), originConnection);
            var targetMetadata = extractionService.parseMetadata(targetConfig.name(), targetConnection);

            var structuralValidationMessages = compareStructure(originMetadata, targetMetadata);
            var volumetricValidationMessages = compareRowCounts(
                    originMetadata,
                    targetMetadata,
                    DatabaseConnectionService.createJdbcTemplate(originConfig),
                    DatabaseConnectionService.createJdbcTemplate(targetConfig)
            );

            return new ConsistencyValidationDataDTO(
                    structuralValidationMessages.isEmpty() && volumetricValidationMessages.isEmpty(),
                    structuralValidationMessages.isEmpty(),
                    volumetricValidationMessages.isEmpty(),
                    Stream.concat(structuralValidationMessages.stream(), volumetricValidationMessages.stream()).toList()
            );
        } catch (SQLException e) {
            throw new ConsistencyValidationException("Erro ao realizar validação de consistência. Detalhes: " + e.getMessage());
        }
    }
}
