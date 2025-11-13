package com.gabrielrq.database_converter.dto;

import java.util.List;

public record ConsistencyValidationDataDTO(
        boolean isConsistent,
        boolean areTablesOk,
        boolean areColumnsOk,
        List<String> messages
) {
}
