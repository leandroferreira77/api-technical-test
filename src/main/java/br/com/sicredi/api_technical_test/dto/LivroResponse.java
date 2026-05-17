package br.com.sicredi.api_technical_test.dto;

import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.model.Livro;

import java.time.LocalDateTime;

public record LivroResponse(
        String id,
        String titulo,
        String autor,
        String isbn,
        Integer anoPublicacao,
        GeneroPojo genero,
        Boolean disponivel,
        LocalDateTime dataInclusao,
        LocalDateTime dataAtualizacao
) {
    public static LivroResponse from(Livro livro) {
        return new LivroResponse(
                livro.getId(),
                livro.getTitulo(),
                livro.getAutor(),
                livro.getIsbn(),
                livro.getAnoPublicacao(),
                livro.getGenero(),
                livro.getDisponivel(),
                livro.getDataInclusao(),
                livro.getDataAtualizacao()
        );
    }
}
