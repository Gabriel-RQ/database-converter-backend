package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Service
public class SqlService {

    @Value("${migration.data.path}")
    private String basePath;
    @Value("${migration.transform.ddl.path}")
    private String ddlPath;
    @Value("${migration.transform.dml.path}")
    private String dmlPath;

    private final JsonService jsonService;

    public SqlService(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    public void generateDML(DatabaseDefinition metadata) {

    }

    public void generateDDL(DatabaseDefinition metadata) {
        // TODO melhorar definição das colunas (quanto aos tipos)
        // TODO garantir ordem de criação das tabelas considerando dependências (com uma árvore de dependências talvez?)

        Path outDir = Path.of(basePath).resolve(metadata.name()).resolve(ddlPath);

        for (var table : metadata.tables()) {
            StringBuilder ddlBuilder = new StringBuilder()
                    .append("CREATE TABLE ")
                    .append(table.schema())
                    .append(".")
                    .append(table.name())
                    .append(" (\n");

            generateDDLColumn(table, ddlBuilder);
            generateDDLPk(table, ddlBuilder);
            generateDDLFk(table, ddlBuilder);
            generateDDLUnique(table, ddlBuilder);

            ddlBuilder.append("\n);");
            write(outDir.resolve(table.schema() + "." + table.name() + ".sql"), ddlBuilder.toString());
        }
    }

    private void generateDDLUnique(TableDefinition table, StringBuilder ddlBuilder) {
        List<String> uniqueDefinitions = new ArrayList<>();
        if (!table.uniqueConstraints().isEmpty()) {
            ddlBuilder.append(",\n");
            for (var unique : table.uniqueConstraints()) {
                String uniqueDef = "UNIQUE " + "(" + String.join(",", unique) + ")";
                uniqueDefinitions.add("\t" + uniqueDef);
            }
        }
        ddlBuilder.append(String.join(",\n", uniqueDefinitions));
    }

    private void generateDDLFk(TableDefinition table, StringBuilder ddlBuilder) {
        List<String> fkDefinitions = new ArrayList<>();
        if (!table.foreignKeys().isEmpty()) {
            ddlBuilder.append(",\n");

            for (var fk : table.foreignKeys()) {
                String fkDef = "FOREIGN KEY (" +
                        String.join(",", fk.localColumns()) +
                        ") REFERENCES " +
                        fk.referencedTable() +
                        " (" +
                        String.join(",", fk.referencedColumns()) +
                        ")";
                fkDefinitions.add("\t" + fkDef);
            }
        }
        ddlBuilder.append(String.join(",\n", fkDefinitions));
    }

    private void generateDDLPk(TableDefinition table, StringBuilder ddlBuilder) {
        if (!table.primaryKeyColumns().isEmpty()) {
            ddlBuilder
                    .append(",\n\t")
                    .append("PRIMARY KEY (").append(String.join(",", table.primaryKeyColumns())).append(")");
        }
    }

    private void generateDDLColumn(TableDefinition table, StringBuilder ddlBuilder) {
        List<String> columnDefinitions = new ArrayList<>();
        for (var column : table.columns()) {
            StringBuilder columnDef = new StringBuilder();
            columnDef
                    .append(column.name())
                    .append(" ")
                    .append(column.targetType());
            switch (column.genericType()) {
                case Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR ->
                        columnDef.append("(").append(column.length()).append(")");
                case Types.NUMERIC, Types.DECIMAL ->
                        columnDef.append("(").append(column.precision()).append(",").append(column.scale()).append(")");

            }
            columnDefinitions.add("\t" + columnDef);
        }

        ddlBuilder.append(String.join(",\n", columnDefinitions));
    }

    public void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());

            try (
                    FileWriter fw = new FileWriter(path.toFile());
                    BufferedWriter writer = new BufferedWriter(fw);
            ) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // // lançar excessão personalizada a ser tratada pela aplicação
        }
    }

}
