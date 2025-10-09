package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.builder.ColumnDefinitionBuilder;
import com.gabrielrq.database_converter.domain.builder.DatabaseDefinitionBuilder;
import com.gabrielrq.database_converter.domain.builder.TableDefinitionBuilder;
import com.gabrielrq.database_converter.service.JsonService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

@Service
public class DataTransformationService {

    private final JsonService jsonService;

    public DataTransformationService(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    public DatabaseDefinition transform(DatabaseDefinition metadata, String target) {
        try {
            Map<Integer, String> targetConversioMap = jsonService.readConversionMap(target);

            DatabaseDefinitionBuilder databaseBuilder = DatabaseDefinitionBuilder.from(metadata).setTables(new ArrayList<>());
            for (var table : metadata.tables()) {
                TableDefinitionBuilder tableBuilder = TableDefinitionBuilder.from(table).setColumns(new ArrayList<>());
                for (var column : table.columns()) {
                    tableBuilder.addColumn(
                            ColumnDefinitionBuilder
                                    .from(column)
                                    .setTargetType(targetConversioMap.getOrDefault(column.genericType(), "INVALID"))
                                    .build()
                    );
                }
                databaseBuilder.addTable(tableBuilder.build());
            }
            var targetMetadata = databaseBuilder.build();
            Path outputPath = Path.of(metadata.name());
            jsonService.write(targetMetadata, outputPath.resolve("target.meta").toString());

            return targetMetadata;
        } catch (IOException e) {
            throw new RuntimeException(e); // lançar excessão personalizada que será tratada pela aplicação
        }
    }

}
