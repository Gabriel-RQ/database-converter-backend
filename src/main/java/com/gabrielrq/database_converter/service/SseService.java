package com.gabrielrq.database_converter.service;

import com.gabrielrq.database_converter.domain.MigrationStatus;
import com.gabrielrq.database_converter.exception.NonExistingSseEmitterException;
import com.gabrielrq.database_converter.mapper.MigrationStatusMapper;
import com.gabrielrq.database_converter.repository.SseEmitterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Service
public class SseService {

    @Value("${migration.sse.timeout:0}")
    private long sseTimeout;

    private final SseEmitterRepository sseEmitterRepository;

    public SseService(SseEmitterRepository sseEmitterRepository) {
        this.sseEmitterRepository = sseEmitterRepository;
    }

    public SseEmitter registerEmitter(UUID id) {
        SseEmitter emitter = new SseEmitter(sseTimeout);

        emitter.onCompletion(() -> sseEmitterRepository.delete(id));
        emitter.onTimeout(() -> sseEmitterRepository.delete(id));
        emitter.onError((e) -> sseEmitterRepository.delete(id));

        sendRegistrationConfirmation(emitter);

        sseEmitterRepository.save(id, emitter);
        return emitter;
    }

    private void sendRegistrationConfirmation(SseEmitter emitter) {
        try {
            emitter.send("Emissor de SSE registrado");
        } catch (IOException ignored) {
        }
    }

    public void sendMigrationStatusUpdate(MigrationStatus status) {
        try {
            SseEmitter emitter = sseEmitterRepository.find(status.getId());
            try {
                emitter.send(SseEmitter.event().name("status").data(MigrationStatusMapper.toMigrationStatusDTO(status), MediaType.APPLICATION_JSON).build());
            } catch (IOException e) {
                emitter.completeWithError(e);
                sseEmitterRepository.delete(status.getId());
            }
        } catch (NonExistingSseEmitterException ignored) {
        }
    }

    public void completeSseEmitter(UUID id) {
        SseEmitter emitter = sseEmitterRepository.find(id);
        emitter.complete();
    }

    @EventListener(ContextClosedEvent.class)
    public void cleanup() {
        sseEmitterRepository.findAll().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("shutdown").data("Servidor encerrando conexão").build());
            } catch (Exception ignored) {}

            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        sseEmitterRepository.clear();
    }
}
