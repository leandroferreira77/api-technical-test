package br.com.sicredi.api_technical_test.service;

import br.com.sicredi.api_technical_test.dto.LivroRequest;
import br.com.sicredi.api_technical_test.dto.LivroResponse;
import br.com.sicredi.api_technical_test.exception.NegocioException;
import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.model.Livro;
import br.com.sicredi.api_technical_test.repository.LivroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository;

    public LivroResponse criar(LivroRequest request) {
        log.debug("Início - criar: isbn={}, titulo={}", request.isbn(), request.titulo());

        validarAnoPublicacao(request.anoPublicacao());

        if (livroRepository.existsByIsbn(request.isbn())) {
            log.warn("Erro - criar: ISBN '{}' já está cadastrado", request.isbn());
            throw new NegocioException("ISBN_DUPLICADO",
                    "ISBN '" + request.isbn() + "' já está cadastrado.", HttpStatus.CONFLICT);
        }

        Livro livro = Livro.builder()
                .titulo(request.titulo())
                .autor(request.autor())
                .isbn(request.isbn())
                .anoPublicacao(request.anoPublicacao())
                .genero(request.genero())
                .disponivel(request.disponivel())
                .dataInclusao(LocalDateTime.now())
                .build();

        LivroResponse response = LivroResponse.from(livroRepository.save(livro));
        log.debug("Fim - criar: livro criado com id={}", response.id());
        return response;
    }

    /**
     * Cache com chave Redis: biblioteca:livro:{id}, TTL de 10 minutos.
     * O corpo do método só é executado em caso de cache miss.
     */
    @Cacheable(cacheNames = "livro", key = "#id")
    public LivroResponse buscarPorId(String id) {
        log.debug("Início - buscarPorId: id={} (cache miss, consultando MongoDB)", id);

        LivroResponse response = livroRepository.findById(id)
                .map(LivroResponse::from)
                .orElseThrow(() -> {
                    log.warn("Erro - buscarPorId: livro id='{}' não encontrado", id);
                    return new NegocioException("LIVRO_NAO_ENCONTRADO",
                            "Livro com id '" + id + "' não encontrado.", HttpStatus.NOT_FOUND);
                });

        log.debug("Fim - buscarPorId: id={}, titulo={}", id, response.titulo());
        return response;
    }

    public Page<LivroResponse> listar(int pagina, int tamanho, GeneroPojo genero) {
        log.debug("Início - listar: pagina={}, tamanho={}, genero={}", pagina, tamanho, genero);

        Pageable pageable = PageRequest.of(pagina, tamanho);
        Page<LivroResponse> resultado;

        if (genero != null) {
            resultado = livroRepository.findByGenero(genero, pageable).map(LivroResponse::from);
        } else {
            resultado = livroRepository.findAll(pageable).map(LivroResponse::from);
        }

        log.debug("Fim - listar: {} registros encontrados (total={})", resultado.getNumberOfElements(), resultado.getTotalElements());
        return resultado;
    }

    @CacheEvict(cacheNames = "livro", key = "#id")
    public LivroResponse atualizar(String id, LivroRequest request) {
        log.debug("Início - atualizar: id={}, isbn={}", id, request.isbn());

        validarAnoPublicacao(request.anoPublicacao());

        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Erro - atualizar: livro id='{}' não encontrado", id);
                    return new NegocioException("LIVRO_NAO_ENCONTRADO",
                            "Livro com id '" + id + "' não encontrado.", HttpStatus.NOT_FOUND);
                });

        if (livroRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
            log.warn("Erro - atualizar: ISBN '{}' já pertence a outro livro", request.isbn());
            throw new NegocioException("ISBN_DUPLICADO",
                    "ISBN '" + request.isbn() + "' já está cadastrado.", HttpStatus.CONFLICT);
        }

        livro.setTitulo(request.titulo());
        livro.setAutor(request.autor());
        livro.setIsbn(request.isbn());
        livro.setAnoPublicacao(request.anoPublicacao());
        livro.setGenero(request.genero());
        livro.setDisponivel(request.disponivel());
        livro.setDataAtualizacao(LocalDateTime.now());

        LivroResponse response = LivroResponse.from(livroRepository.save(livro));
        log.debug("Fim - atualizar: id={} atualizado, cache invalidado", id);
        return response;
    }

    @CacheEvict(cacheNames = "livro", key = "#id")
    public void deletar(String id) {
        log.debug("Início - deletar: id={}", id);

        if (!livroRepository.existsById(id)) {
            log.warn("Erro - deletar: livro id='{}' não encontrado", id);
            throw new NegocioException("LIVRO_NAO_ENCONTRADO",
                    "Livro com id '" + id + "' não encontrado.", HttpStatus.NOT_FOUND);
        }

        livroRepository.deleteById(id);
        log.debug("Fim - deletar: id={} removido, cache invalidado", id);
    }

    private void validarAnoPublicacao(Integer ano) {
        if (ano > Year.now().getValue()) {
            log.warn("Erro - validarAnoPublicacao: ano={} é maior que o ano atual={}", ano, Year.now().getValue());
            throw new NegocioException("ANO_PUBLICACAO_INVALIDO",
                    "Ano de publicação não pode ser maior que o ano atual (" + Year.now().getValue() + ").",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
