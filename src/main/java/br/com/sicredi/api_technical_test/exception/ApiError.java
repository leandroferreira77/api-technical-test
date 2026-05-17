package br.com.sicredi.api_technical_test.exception;

import java.time.LocalDateTime;

public record ApiError(String codigo, String mensagem, LocalDateTime timestamp) {}
