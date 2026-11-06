package com.example.demo.infrastructure.entity.enums;

public enum PedidoStatus {
    AGUARDANDO_PAGAMENTO,
    PAGAMENTO_APROVADO,
    EM_PREPARACAO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    // Define o fluxo permitido do pedido
    public boolean podeMudarPara(PedidoStatus novoStatus) {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> novoStatus == PAGAMENTO_APROVADO || novoStatus == CANCELADO;
            case PAGAMENTO_APROVADO -> novoStatus == EM_PREPARACAO || novoStatus == CANCELADO;
            case EM_PREPARACAO -> novoStatus == ENVIADO;
            case ENVIADO -> novoStatus == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
