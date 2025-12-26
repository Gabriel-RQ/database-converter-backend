package com.gabrielrq.database_converter.util.migration;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Supplier;

public class MigrationLogger {

    public static <T> T withMigration(UUID id, Supplier<T> supplier) {
        MDC.put("migrationId", id.toString());
        try {
            return supplier.get();
        } finally {
            MDC.clear();
        }
    }

    public static void withMigration(UUID id, Runnable runnable) {
        MDC.put("migrationId", id.toString());
        try {
            runnable.run();
        } finally {
            MDC.clear();
        }
    }
}
