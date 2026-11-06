package com.example.demo.controller.dtos.request;

import com.example.demo.infrastructure.entity.enums.PedidoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PedidoStatusRequestDTO(
        @Schema(example = "PAGAMENTO_APROVADO")
        @NotNull
        PedidoStatus status
) {
}
