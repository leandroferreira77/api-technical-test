package br.com.sicredi.api_technical_test.repository;

import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.model.Livro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LivroRepository extends MongoRepository<Livro, String> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, String id);

    Page<Livro> findByGenero(GeneroPojo genero, Pageable pageable);
}
