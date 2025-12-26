package com.gabrielrq.database_converter.repository;

import com.gabrielrq.database_converter.exception.NonExistingSseEmitterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SseEmitterRepositoryTest {

    private SseEmitterRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SseEmitterRepository();
    }

    @Test
    @DisplayName("Deve salvar e recuperar um SseEmitter pelo ID")
    void shouldSaveAndFindEmitter() {
        // Arrange
        UUID id = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();

        // Act
        repository.save(id, emitter);
        SseEmitter foundEmitter = repository.find(id);

        // Assert
        assertThat(foundEmitter).isNotNull();
        assertThat(foundEmitter).isEqualTo(emitter);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar um ID inexistente")
    void shouldThrowExceptionWhenEmitterNotFound() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act & Assert
        NonExistingSseEmitterException exception = assertThrows(NonExistingSseEmitterException.class, () -> {
            repository.find(id);
        });

        assertThat(exception.getMessage()).contains("Nenhum emissor de SSE vinculado ao ID");
    }

    @Test
    @DisplayName("Deve deletar um SseEmitter pelo ID")
    void shouldDeleteEmitter() {
        // Arrange
        UUID id = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        repository.save(id, emitter);

        // Act
        repository.delete(id);

        // Assert
        assertThrows(NonExistingSseEmitterException.class, () -> {
            repository.find(id);
        });
    }

    @Test
    @DisplayName("Deve listar todos os SseEmitters")
    void shouldFindAllEmitters() {
        // Arrange
        repository.save(UUID.randomUUID(), new SseEmitter());
        repository.save(UUID.randomUUID(), new SseEmitter());
        repository.save(UUID.randomUUID(), new SseEmitter());

        // Act
        List<SseEmitter> allEmitters = repository.findAll();

        // Assert
        assertThat(allEmitters).hasSize(3);
    }

    @Test
    @DisplayName("Deve limpar todos os registros do repositório")
    void shouldClearRepository() {
        // Arrange
        repository.save(UUID.randomUUID(), new SseEmitter());
        repository.save(UUID.randomUUID(), new SseEmitter());

        // Act
        repository.clear();

        // Assert
        assertThat(repository.findAll()).isEmpty();
    }
}