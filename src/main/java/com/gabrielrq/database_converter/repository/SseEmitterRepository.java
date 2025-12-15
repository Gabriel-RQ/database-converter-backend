package com.gabrielrq.database_converter.repository;

import com.gabrielrq.database_converter.exception.NonExistingSseEmitterException;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseEmitterRepository {
    private final Map<UUID, SseEmitter> repo = new ConcurrentHashMap<>();

    public void save(UUID id, SseEmitter emitter) {
        repo.put(id, emitter);
    }

    public void delete(UUID id) {
        repo.remove(id);
    }

    public SseEmitter find(UUID id) {
        SseEmitter emitter = repo.get(id);
        if (emitter == null) {
            throw new NonExistingSseEmitterException("Nenhum emissor de SSE vinculado ao ID '" + id + "' encontrado. Experimente abrir uma conexão SSE para essa migração.");
        }
        return emitter;
    }

    public List<SseEmitter> findAll() {
        return repo.values().stream().toList();
    }

    public void clear() {
        repo.clear();
    }
}
