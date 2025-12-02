# Database Converter API

**[English]**

This repository contains the backend implementation for the Database Converter API. It's built with Java and SpringBoot.

This project was first developed as part of my course completion paper.

A frontend implementation for this API can be found [here](https://github.com/Gabriel-RQ/database-converter-frontend).

## How to run

1. Clone this repository:
```bash
git clone https://github.com/Gabriel-RQ/database-converter-backend
```
2. Build the artifact:
```bash
mvn clean package -DskipTests
```
3. Execute the server:
```bash
java -jar target/database-converter-api-1.0.0.jar
# Artifact name may change with version updates!
```

## API

The following routes are available:
```bash
POST /api/v1/migrations - Starts a new migration
POST /api/v1/migrations/{id}/extract - Starts the extraction step
POST /api/v1/migrations/{id}/transform - Starts the transformation step
POST /api/v1/migrations/{id}/load - Starts the loading step
POST /api/v1/migrations/{id}/validate - Starts the validation step
GET /api/v1/migrations/{id}/status - Retrieves the status of a migration
GET /api/v1/migrations/{id}/sse - Retrieves updates through Server Sent Events for a migration
GET /api/v1/migrations/{id}/sql - Retrieves DDL SQL files generated for a migration (paginated)
PUT /api/v1/migrations/{id}/sql - Updates DDL SQL files generated for a migration
```

**[Português]**

Esse repositório contém a implementação do backend da API de Conversão de Bases de dados. Utiliza Java e SpringBoot.

Esse projeto foi desenvolvido inicialmente como parte de meu Trabalho de Conclusão de Curso.

A implementação de um frontend para essa API pode ser encontrada [aqui](https://github.com/Gabriel-RQ/database-converter-frontend).

## How to run

1. Clone esse repositório:
```bash
git clone https://github.com/Gabriel-RQ/database-converter-backend
```
2. Faça o build do artefato java:
```bash
mvn clean package -DskipTests
```
3. Execute o servidor:
```bash
java -jar target/database-converter-api-1.0.0.jar
# O nome do artefato pode mudar com atualizações de versão!
```

## API

As seguintes rotas estão disponíveis:
```bash
POST /api/v1/migrations - Inicia uma nova migração
POST /api/v1/migrations/{id}/extract - Inicia a etapa de extração
POST /api/v1/migrations/{id}/transform - Inicia a etapa de transformação
POST /api/v1/migrations/{id}/load - Inicia a etapa de carga
POST /api/v1/migrations/{id}/validate - Inicia a etapa de validação
GET /api/v1/migrations/{id}/status - Retorna o status de uma migração
GET /api/v1/migrations/{id}/sse - Retorna atualizações de uma migração por meio de Server Sent Events
GET /api/v1/migrations/{id}/sql - Retorna arquivos SQL DDL gerados para uma migração (paginado)
PUT /api/v1/migrations/{id}/sql - Atualiza os arquivos SQL DDL gerados para uma migração
```
