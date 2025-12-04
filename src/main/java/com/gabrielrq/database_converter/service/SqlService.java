package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.ColumnDefinition;
import com.gabrielrq.database_converter.domain.DatabaseDefinition;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.dto.SqlDTO;
import com.gabrielrq.database_converter.dto.SqlPageDTO;
import com.gabrielrq.database_converter.exception.SqlException;
import com.gabrielrq.database_converter.util.FirebirdBlobHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());

            try (
                    FileWriter fw = new FileWriter(path.toFile());
                    BufferedWriter writer = new BufferedWriter(fw)
            ) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new SqlException("Erro ao escrever arquivo SQL. Detalhes: " + e.getMessage());
        }
    }

    public String read(Path path) throws IOException {
        Path p = Path.of(basePath).resolve(path);

        if (!Files.exists(p)) {
            throw new FileNotFoundException("File '" + p + "' not found");
        }

        return Files.readString(p);
    }

    public SqlPageDTO listDDL(String name, int page, int size) {
        Path dir = Path.of(basePath).resolve(name).resolve(ddlPath);

        List<SqlDTO> pageFiles = new ArrayList<>();
        int total = 0;
        int start = page * size;
        int end = start + size;

        try (Stream<Path> filesStream = Files.list(dir)) {
            Iterator<Path> iterator = filesStream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".sql"))
                    .sorted()
                    .iterator();

            while (iterator.hasNext()) {
                Path path = iterator.next();
                if (total >= start && total < end) {
                    String content = Files.readString(path);
                    pageFiles.add(new SqlDTO(path.getFileName().toString(), content));
                }
                total++;
            }

            return new SqlPageDTO(page, size, total, pageFiles);

        } catch (IOException e) {
            throw new SqlException("Erro ao listar arquivos. Detalhes: " + e.getMessage());
        }
    }

    public void updateDDL(String name, List<SqlDTO> sqlFiles) {
        Path path = Path.of(basePath).resolve(name).resolve(ddlPath);

        for (var file : sqlFiles) {
            write(path.resolve(file.filename()), file.content());
        }
    }

    public void bufferReadAndExec(Path path, Statement statement) throws IOException, SQLException {
        Path p = Path.of(basePath).resolve(path);

        if (!Files.exists(p)) {
            throw new FileNotFoundException("Arquivo '" + p + "' não encontrado.");
        }

        try (BufferedReader br = Files.newBufferedReader(p)) {
            StringBuilder sqlBlock = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;
                sqlBlock.append(line).append(" ");

                if (line.endsWith(";")) {
                    statement.execute(sqlBlock.toString());
                    sqlBlock.setLength(0);
                }
            }

            if (!sqlBlock.isEmpty()) {
                statement.execute(sqlBlock.toString());
            }
        }
    }

    public void generate(DatabaseDefinition metadata, Map<Integer, String> conversionMap, String target) {
        generateDDL(metadata, conversionMap, target);
        generateDML(metadata, target);
    }

    public void generateDML(DatabaseDefinition metadata, String target) {
        Path outDir = Path.of(basePath).resolve(metadata.name()).resolve(dmlPath);
        Path tablesPath = Path.of(basePath).resolve(metadata.name()).resolve("tables");

        for (var table : metadata.tables()) {
            StringBuilder dmlBuilder = new StringBuilder();
            String columns = String.join(",",
                    table.columns().stream()
                            .filter(c -> !"INVALID".equalsIgnoreCase(c.targetType()))
                            .map(ColumnDefinition::name).toList()
            );

            if (!generateDMLData(table, tablesPath, dmlBuilder, columns, target)) {
                continue;
            }

            write(outDir.resolve(/* table.schema() + "." + */ table.name() + ".sql"), dmlBuilder.toString());
        }
    }

    public void generateDDL(DatabaseDefinition metadata, Map<Integer, String> conversionMap, String target) {
        // TODO melhorar definição das colunas (quanto aos tipos)

        Path outDir = Path.of(basePath).resolve(metadata.name()).resolve(ddlPath);

        for (var table : metadata.tables()) {
            StringBuilder ddlBuilder = new StringBuilder()
                    .append("CREATE TABLE ")
//                    .append(table.schema())
//                    .append(".")
//                    .append("'")
                    .append(table.name())
//                    .append("'")
                    .append(" (\n");

            generateDDLColumn(table, ddlBuilder, conversionMap, target);
            generateDDLPk(table, ddlBuilder);
            generateDDLFk(table, ddlBuilder);
            generateDDLUnique(table, ddlBuilder);

            ddlBuilder.append("\n);");
            write(outDir.resolve(/* table.schema() + "." + */ table.name() + ".sql"), ddlBuilder.toString());
        }
    }

    private boolean generateDMLData(TableDefinition table, Path tablesPath, StringBuilder dmlBuilder, String columns, String target) {
        try {
            List<Map<String, Object>> tableData = jsonService.readTableData(tablesPath.resolve(/* table.schema() + "." + */ table.name() + ".json"));
            if (tableData.isEmpty()) return false;

            for (var data : tableData) {
                String values = data.entrySet().stream()
                        .filter(
                                e -> table.columns().stream()
                                        .anyMatch(
                                                c -> c.name().equalsIgnoreCase(e.getKey()) && !"INVALID".equalsIgnoreCase(c.targetType())
                                        )
                        )
                        .map(e -> formatDMLValue(e.getValue(), target))
                        .collect(Collectors.joining(","));

                dmlBuilder.append("INSERT INTO ")
//                        .append(table.schema())
//                        .append(".")
                        .append(table.name())
                        .append(" (")
                        .append(columns)
                        .append(") VALUES ")
                        .append("(")
                        .append(values)
                        .append(");")
                        .append(System.lineSeparator());
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new SqlException("Erro ao gerar DML para a tabela '" + table.name() + "'. Detalhes: " + e.getMessage());
        }
        return true;
    }

    private String formatDMLValue(Object value, String target) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            String stringValue = value.toString()
                    .replace("'", "''")
                    .replaceAll("[\\u0000-\\u001F\\u007F]", ""); // Remove caracteres de controle

            if ("FIREBIRD".equals(target)) {
                if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")) {
                    stringValue = stringValue
                            .replace("T", " ")
                            .replace("Z", "").replaceAll("(\\.\\d{3})\\d{3}$", "$1");
                    ;
                } else if (stringValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}(\\.\\d+)?")) {
                    stringValue = stringValue.substring(0, 19)
                            .replace("T", " ")
                            .replaceAll("(\\.\\d{3})\\d{3}$", "$1");
                    ;
                }

                if (stringValue.length() > 60_000) {
                    return FirebirdBlobHelper.toFirebirdBlobLiteral(stringValue);
                }
            }

            return "'" + stringValue + "'";
        }
    }

    private String formatColumnType(ColumnDefinition column, Map<Integer, String> conversionMap, String target) {
        String type = column.targetType();
        return switch (column.genericType()) {
            case Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CHAR -> {
                if ("TEXT".equalsIgnoreCase(column.originType()) || column.length() >= Integer.MAX_VALUE) {
                    yield conversionMap.getOrDefault(-1, "TEXT");
                }
                if (column.length() > 0) {
                    if ("FIREBIRD".equals(target)) {
                        yield type + "(" + column.length() * 3 + ")";
                    }
                    yield type + "(" + column.length() + ")";
                }
                yield type;
            }
            case Types.NUMERIC, Types.DECIMAL -> {
                if (column.precision() > 0) {
                    yield type + "(" + column.precision() + "," + column.scale() + ")";
                }
                yield type;
            }
            default -> type;
        };
    }

    private boolean isStringType(ColumnDefinition column) {
        return switch (column.genericType()) {
            case Types.VARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CHAR, Types.NCHAR -> true;
            default -> false;
        };
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

    private void generateDDLColumn(TableDefinition table, StringBuilder ddlBuilder, Map<Integer, String> conversionMap, String target) {
        List<String> columnDefinitions = new ArrayList<>();
        var validColumns = table.columns().stream().filter(c -> !"INVALID".equalsIgnoreCase(c.targetType())).toList();
        for (var column : validColumns) {
            StringBuilder columnDef = new StringBuilder();
            columnDef
                    .append(column.name())
                    .append(" ")
                    .append(formatColumnType(column, conversionMap, target));

//            if (column.defaultValue() != null && !column.defaultValue().isBlank()) {
//                columnDef.append(" DEFAULT ");
//                if (isStringType(column)) {
//                    columnDef.append("'").append(column.defaultValue()).append("'");
//                } else {
//                    columnDef.append(column.defaultValue());
//                }
//            }

            if (!column.isNullable()) {
                columnDef.append(" NOT NULL");
            }

            columnDefinitions.add("\t" + columnDef);
        }

        ddlBuilder.append(String.join(",\n", columnDefinitions));
    }
}
