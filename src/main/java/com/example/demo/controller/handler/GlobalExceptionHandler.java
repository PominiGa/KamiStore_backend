package com.example.demo.controller.handler;

import com.example.demo.controller.dtos.response.ErroResponseDTO;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Converte as exceções em respostas JSON padronizadas (e documenta os códigos de erro no Swagger)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponseDTO naoEncontrado(Exception e, HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponseDTO conflito(ConflictException e, HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, e.getMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponseDTO integridade(DataIntegrityViolationException e, HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, "Operação viola uma restrição do banco de dados (registro duplicado ou em uso)", request, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponseDTO regraNegocio(RegraNegocioException e, HttpServletRequest request) {
        return erro(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponseDTO validacao(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos", request, campos);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, PropertyReferenceException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponseDTO requisicaoInvalida(Exception e, HttpServletRequest request) {
        String mensagem = e instanceof HttpMessageNotReadableException
                ? "Corpo da requisição inválido ou mal formatado"
                : e.getMessage();
        return erro(HttpStatus.BAD_REQUEST, mensagem, request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ErroResponseDTO arquivoGrande(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return erro(HttpStatus.CONTENT_TOO_LARGE, "Arquivo muito grande: cada imagem pode ter no máximo 5MB", request, null);
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponseDTO multipartInvalido(Exception e, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "Envie as imagens como multipart/form-data no campo 'imagens'", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponseDTO naoAutenticado(AuthenticationException e, HttpServletRequest request) {
        return erro(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroResponseDTO acessoNegado(AccessDeniedException e, HttpServletRequest request) {
        // "Access Denied" é a mensagem padrão do @PreAuthorize (perfil sem permissão)
        String mensagem = "Access Denied".equals(e.getMessage())
                ? "Seu perfil não tem permissão para acessar este recurso"
                : e.getMessage();
        return erro(HttpStatus.FORBIDDEN, mensagem, request, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResponseDTO erroInesperado(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", request, null);
    }

    private ErroResponseDTO erro(HttpStatus status, String mensagem, HttpServletRequest request,
                                 Map<String, String> campos) {
        return new ErroResponseDTO(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, request.getRequestURI(), campos);
    }
}
