package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.ColumnDefinition;
import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.domain.TransformationResult;
import com.gabrielrq.database_converter.service.JsonService;
import com.gabrielrq.database_converter.service.SqlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataTransformationServiceTest {

    @Mock
    private JsonService jsonService;

    @Mock
    private SqlService sqlService;

    @InjectMocks
    private DataTransformationService transformationService;

    @Test
    @DisplayName("Deve transformar metadados aplicando mapa de conversão")
    void shouldTransformMetadata() throws IOException {
        // Arrange
        String identifier = "migration-123";
        String target = "POSTGRES";

        ColumnDefinition colOrigin = new ColumnDefinition(
                "name", 12, "VARCHAR", null, 100, 0, 0, true, false, null, 1
        );
        TableDefinition tableOrigin = new TableDefinition(
                "user", List.of(colOrigin), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "public"
        );
        DatabaseDefinition metadataOrigin = new DatabaseDefinition(
                "db_origin", "public", List.of(tableOrigin), "utf-8"
        );

        when(jsonService.readConversionMap(target)).thenReturn(Map.of(12, "TEXT"));

        // Act
        TransformationResult result = transformationService.transform(identifier, metadataOrigin, target);

        // Assert
        assertThat(result.metadata().tables()).hasSize(1);

        ColumnDefinition colTransformed = result.metadata().tables().getFirst().columns().getFirst();
        assertThat(colTransformed.targetType()).isEqualTo("TEXT");

        verify(jsonService).write(any(DatabaseDefinition.class),  org.mockito.ArgumentMatchers.contains("target.meta"));
        verify(sqlService).generate(eq(identifier), any(DatabaseDefinition.class), any(), eq(target));
    }
}