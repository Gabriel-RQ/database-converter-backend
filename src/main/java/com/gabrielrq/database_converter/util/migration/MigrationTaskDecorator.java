package com.gabrielrq.database_converter.util.migration;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MigrationTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        final var contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
