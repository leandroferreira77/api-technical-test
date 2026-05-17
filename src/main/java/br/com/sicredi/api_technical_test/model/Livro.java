package br.com.sicredi.api_technical_test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "livros")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Livro {

    @Id
    private String id;

    private String titulo;

    private String autor;

    @Indexed(unique = true)
    private String isbn;

    private Integer anoPublicacao;

    private GeneroPojo genero;

    private Boolean disponivel;

    private LocalDateTime dataInclusao;

    private LocalDateTime dataAtualizacao;
}
