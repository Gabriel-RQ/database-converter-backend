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
    Map<UUID, SseEmitter> repo = new ConcurrentHashMap<>();

    public synchronized void save(UUID id, SseEmitter emitter) {
        repo.put(id, emitter);
    }

    public synchronized void delete(UUID id) {
        repo.remove(id);
    }

    public SseEmitter find(UUID id) {
        if (!repo.containsKey(id)) {
            throw new NonExistingSseEmitterException("Nenhum emissor de SSE vinculado ao ID '" + id + "' encontrado. Experimente abrir uma conexão SSE para essa migração.");
        }
        return repo.get(id);
    }

    public List<SseEmitter> findAll() {
        return repo.values().stream().toList();
    }

    public synchronized void clear() {
        repo.clear();
    }
}
