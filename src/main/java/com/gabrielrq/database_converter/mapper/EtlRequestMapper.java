package com.gabrielrq.database_converter.mapper;

import com.gabrielrq.database_converter.domain.EtlRequest;
import com.gabrielrq.database_converter.dto.etl.ExtractionRequestDTO;
import com.gabrielrq.database_converter.dto.etl.LoadRequestDTO;
import com.gabrielrq.database_converter.dto.etl.TransformationRequestDTO;
import com.gabrielrq.database_converter.dto.etl.ValidationRequestDTO;

public class EtlRequestMapper {

    public static EtlRequest fromExtractionRequestDTO(ExtractionRequestDTO extractionRequestDTO) {
        return new EtlRequest(
                extractionRequestDTO.id(),
                extractionRequestDTO.originConfig(),
                null,
                null
        );
    }

    public static EtlRequest fromTransformationRequestDTO(TransformationRequestDTO transformationRequestDTO) {
        return new EtlRequest(
                transformationRequestDTO.id(),
                null,
                null,
                transformationRequestDTO.target()
        );
    }

    public static EtlRequest fromLoadRequestDTO(LoadRequestDTO loadRequestDTO) {
        return new EtlRequest(
                loadRequestDTO.id(),
                null,
                loadRequestDTO.targetConfig(),
                null
        );
    }

    public static EtlRequest fromValidationRequestDTO(ValidationRequestDTO validationRequestDTO) {
        return new EtlRequest(
                validationRequestDTO.id(),
                validationRequestDTO.originConfig(),
                validationRequestDTO.targetConfig(),
                null
        );
    }
}
