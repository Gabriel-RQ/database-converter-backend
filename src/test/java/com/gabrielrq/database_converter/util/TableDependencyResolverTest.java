package com.gabrielrq.database_converter.util;

import com.gabrielrq.database_converter.domain.ForeignKeyDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TableDependencyResolverTest {

    @Test
    @DisplayName("Deve ordenar tabelas corretamente baseando-se nas chaves estrangeiras")
    void shouldSortTablesByDependency() {
        // Arrange
        TableDefinition userTable = createTable("user", Collections.emptyList());
        ForeignKeyDefinition userFk = new ForeignKeyDefinition("fk_user", "user", List.of("user_id"), List.of("id"));
        TableDefinition orderTable = createTable("order", List.of(userFk));
        List<TableDefinition> input = List.of(userTable, orderTable);

        // Act
        List<TableDefinition> result = TableDependencyResolver.sortTablesByDependency(input);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).isEqualTo(userTable);
        assertThat(result.get(1)).isEqualTo(orderTable);
    }

    @Test
    @DisplayName("Deve lançar exceção quando detectar dependência circular")
    void shouldThrowExceptionOnCircularDependency() {
        // Arrange
        ForeignKeyDefinition fkToB = new ForeignKeyDefinition("fk_b", "table_b", List.of("b_id"), List.of("id"));
        TableDefinition tableA = createTable("table_a", List.of(fkToB));
        ForeignKeyDefinition fkToA = new ForeignKeyDefinition("fk_a", "table_a", List.of("a_id"), List.of("id"));
        TableDefinition tableB = createTable("table_b", List.of(fkToA));
        List<TableDefinition> input = List.of(tableA, tableB);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            TableDependencyResolver.sortTablesByDependency(input);
        });

        assertThat(exception.getMessage()).contains("Ciclo detectado");
    }

    private TableDefinition createTable(String name, List<ForeignKeyDefinition> fks) {
        return new TableDefinition(name, Collections.emptyList(), Collections.emptyList(), fks, Collections.emptyList(), "public");
    }
}
