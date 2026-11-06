package com.example.demo.infrastructure.exceptions;

// Lançada quando a requisição é válida, mas viola uma regra do negócio
// (ex.: estoque insuficiente, carrinho vazio, transição de status inválida)
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
