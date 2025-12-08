package com.gabrielrq.database_converter.service.etl;

import com.gabrielrq.database_converter.domain.TableDefinition;
import com.gabrielrq.database_converter.dto.DbConnectionConfigDTO;
import com.gabrielrq.database_converter.domain.TransformationResult;
import com.gabrielrq.database_converter.exception.LoadingException;
import com.gabrielrq.database_converter.service.DatabaseConnectionService;
import com.gabrielrq.database_converter.service.SqlService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Service
public class DataLoadingService {

    private final SqlService sqlService;

    public DataLoadingService(SqlService sqlService) {
        this.sqlService = sqlService;
    }

    public void load(String identifier, DbConnectionConfigDTO config, TransformationResult transformationOutput) {
        Path basePath = Path.of(identifier);
        JdbcTemplate template = DatabaseConnectionService.createJdbcTemplate(config);
        executeDDL(transformationOutput.executionList(), basePath, template);
        executeDML(transformationOutput.executionList(), basePath, config);
    }

    private void executeDDL(List<TableDefinition> executionList, Path basePath, JdbcTemplate template) {
        for (var table : executionList) {
            try {
                String sql = sqlService.read(basePath.resolve("ddl").resolve(/* table.schema() + "."  + */ table.name() + ".sql"));
                template.execute(sql);
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                throw new LoadingException("Erro na carga de dados. Detalhe: " + e.getMessage());
            }
        }
    }

    private void executeDML(List<TableDefinition> executionList, Path basePath, DbConnectionConfigDTO config) {
        try (
                Connection connection = DatabaseConnectionService.createConnection(config);
                Statement stmt = connection.createStatement()
        ) {
            for (final var table : executionList) {
                try {
                    Path dmlPath = basePath.resolve("dml").resolve(/* table.schema() + "." + */ table.name() + ".sql");
                    sqlService.bufferReadAndExec(dmlPath, stmt);
                    connection.commit();
                } catch (FileNotFoundException ignored) {
                } catch (SQLException e) {
                    throw new LoadingException("Erro ao executar DML para tabela '" + table.name() + "'. Detalhe: " + e.getMessage());
                }
            }
        } catch (IOException | SQLException e) {
            throw new LoadingException("Erro ao executar DML. Detalhe: " + e.getMessage());
        }
    }
}
