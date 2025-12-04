package com.gabrielrq.database_converter.service;


import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.exception.JsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JsonService {

    private static final Logger log = LoggerFactory.getLogger(JsonService.class);
    @Value("${migration.data.path}")
    private String basePath;
    @Value("${migration.transform.maps.path}")
    private String conversionMapsPath;

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonService() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public void write(Object object, String filename) {
        Path outputDir = Path.of(basePath);
        Path outputFile = outputDir.resolve(filename + ".json");
        try {
            Files.createDirectories(outputFile.getParent());
            if (Files.exists(outputFile)) {
                Files.delete(outputFile);
            }
            mapper.writeValue(outputFile.toFile(), object);
        } catch (IOException e) {
            throw new JsonException("Erro ao escrever JSON. Detalhes: " + e.getMessage());
        }
    }

    public void writeStream(ResultSet rs, String filename, TableDefinition table) {
        Path outputDir = Path.of(basePath);
        Path outputFile = outputDir.resolve(filename + ".json");

        try {
            if (!rs.next()) {
                return;
            }

            ResultSetMetaData metadata = rs.getMetaData();
            int columns = metadata.getColumnCount();
            String[] columnNames = new String[columns];

            for (int i = 1; i <= columns; i++) {
                columnNames[i - 1] = metadata.getColumnName(i);
            }

            Files.createDirectories(outputFile.getParent());

            try (
                    FileOutputStream fos = new FileOutputStream(outputFile.toFile());
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    JsonGenerator generator = mapper.getFactory().createGenerator(bos, JsonEncoding.UTF8);
            ) {
                generator.writeStartArray();

                do {
                    generator.writeStartObject();

                    for (int i = 1; i <= columns; i++) {
                        String columnName = columnNames[i - 1];
                        int columnType = metadata.getColumnType(i);
                        Object value = rs.getObject(i);

                        switch (columnType) {
                            case Types.DATE -> {
                                Date date = rs.getDate(i);
                                generator.writeStringField(columnName, date != null ? date.toLocalDate().toString() : null);
                            }
                            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> {
                                Timestamp ts = rs.getTimestamp(i);
                                generator.writeStringField(columnName, ts != null ? ts.toInstant().toString() : null);
                            }
                            case Types.CLOB -> {
                                Clob clob = rs.getClob(i);
                                String clobValue = clob != null ? clob.getSubString(1L, (int) clob.length()) : null;
                                generator.writeStringField(columnName, clobValue);
                            }
                            case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> {
                                byte[] bytes = rs.getBytes(i);
                                if (bytes != null) {
                                    generator.writeBinaryField(columnName, bytes); // writes as base64
                                } else {
                                    generator.writeNullField(columnName);
                                }
                            }
                            case Types.ARRAY -> {
                                try {
                                    Object[] arr = (Object[]) ((Array) value).getArray();
                                    String arrStr = Arrays.stream(arr).map(v -> v != null ? v.toString() : "").collect(Collectors.joining(";"));
                                    generator.writeStringField(columnName, arrStr);
                                } catch (SQLException ignored) {
                                    /* Arrays are poorly supported in some DBMS, so: try to convert it to a comma separated text, on fail ignore */
                                    table.columns().remove(columnName);
                                }
                            }
                            case Types.OTHER, Types.DISTINCT -> {
                                /* Ignore database specific types */
                                table.columns().remove(columnName);
                            }
                            default -> {
                                if (value == null) {
                                    generator.writeNullField(columnName);
                                } else {
                                    generator.writeObjectField(columnName, value);
                                }
                            }
                        }
                    }

                    generator.writeEndObject();
                } while (rs.next());

                generator.writeEndArray();
                generator.flush();
            }
        } catch (IOException | SQLException e) {
            throw new JsonException("Erro ao escrever JSON. Detalhes: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> readTableData(Path tablePath) throws IOException {
        if (!Files.exists(tablePath)) {
            throw new FileNotFoundException("File '" + tablePath + "' not found");
        }

        try (FileInputStream stream = new FileInputStream(tablePath.toString())) {
            return mapper.readValue(stream, new TypeReference<List<Map<String, Object>>>() {
            });
        }
    }

    public Map<Integer, String> readConversionMap(String mapName) throws IOException {
        Path mapPath = Path.of(conversionMapsPath).resolve(mapName + ".json");

        try (InputStream stream = JsonService.class.getClassLoader().getResourceAsStream(mapPath.toString())) {
            return mapper.readValue(stream, new TypeReference<Map<Integer, String>>() {
            });
        }
    }
}