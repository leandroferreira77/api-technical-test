package br.com.sicredi.api_technical_test.dto;

import br.com.sicredi.api_technical_test.model.GeneroPojo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LivroRequest(

        @NotBlank(message = "Título é obrigatório")
        String titulo,

        @NotBlank(message = "Autor é obrigatório")
        String autor,

        @NotBlank(message = "ISBN é obrigatório")
        String isbn,

        @NotNull(message = "Ano de publicação é obrigatório")
        @Min(value = 1001, message = "Ano de publicação deve ser maior que 1000")
        Integer anoPublicacao,

        @NotNull(message = "Gênero é obrigatório")
        GeneroPojo genero,

        @NotNull(message = "Disponibilidade é obrigatória")
        Boolean disponivel
) {}
