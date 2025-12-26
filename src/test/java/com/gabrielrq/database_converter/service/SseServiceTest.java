package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.repository.SseEmitterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    @Mock
    private SseEmitterRepository repository;

    @InjectMocks
    private SseService sseService;

    @Test
    @DisplayName("Deve registrar um novo emissor SSE")
    void shouldRegisterEmitter() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        SseEmitter emitter = sseService.registerEmitter(id);

        // Assert
        assertThat(emitter).isNotNull();
        verify(repository).save(eq(id), any(SseEmitter.class));
    }

    @Test
    @DisplayName("Deve enviar atualização de status se o emissor existir")
    void shouldSendStatusUpdate() {
        // Arrange
        UUID id = UUID.randomUUID();
        MigrationStatus status = new MigrationStatus();
        status.setId(id);
        status.setName("Test Migration");

        SseEmitter mockEmitter = mock(SseEmitter.class);
        when(repository.find(id)).thenReturn(mockEmitter);

        // Act
        sseService.sendMigrationStatusUpdate(status);

        // Assert
        try {
            verify(mockEmitter).send(any(Set.class));
        } catch (java.io.IOException ignored) {
        }
    }
}
