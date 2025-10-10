package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.DatabaseDefinition;
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
        // TODO
    }

    public void generateDDL(DatabaseDefinition metadata) {
        Path outDir = Path.of(basePath).resolve(metadata.name()).resolve(ddlPath);

        for (var table : metadata.tables()) {
            StringBuilder ddlBuilder = new StringBuilder();
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

            ddlBuilder
                    .append("CREATE TABLE ")
                    .append(table.schema())
                    .append(".")
                    .append(table.name())
                    .append(" (\n")
                    .append(String.join(",\n", columnDefinitions));

            if (!table.primaryKeyColumns().isEmpty()) {
                ddlBuilder.append(",\n\t");
                ddlBuilder.append("PRIMARY KEY (").append(String.join(",", table.primaryKeyColumns())).append(")");
            }

            ddlBuilder.append("\n);");
            write(outDir.resolve(table.schema() + "." + table.name() + ".sql"), ddlBuilder.toString());
        }
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
