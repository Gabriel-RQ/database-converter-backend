package com.gabrielrq.database_converter.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FirebirdBlobHelper {

    public static String toFirebirdBlobLiteral(String text) {
        if (text == null || text.isEmpty()) {
            return "NULL";
        }

        // Converte o texto para bytes UTF-8
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        // Converte cada byte para HEX
        StringBuilder hexBuilder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexBuilder.append(String.format("%02X", b));
        }
        String hex = hexBuilder.toString();

        // Divide em chunks seguros (< 64 KB)
        int chunkSize = 30_000;
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < hex.length(); i += chunkSize) {
            chunks.add(hex.substring(i, Math.min(hex.length(), i + chunkSize)));
        }

        // Gera o literal final concatenado com ||
        return String.join(" || ", chunks.stream().map(s -> "CAST(x'" + s + "' AS BLOB SUB_TYPE TEXT)").toArray(String[]::new));
    }
}
