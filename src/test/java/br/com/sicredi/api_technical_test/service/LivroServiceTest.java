package br.com.sicredi.api_technical_test.service;

import br.com.sicredi.api_technical_test.dto.LivroRequest;
import br.com.sicredi.api_technical_test.dto.LivroResponse;
import br.com.sicredi.api_technical_test.exception.NegocioException;
import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.model.Livro;
import br.com.sicredi.api_technical_test.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LivroService - Testes unitários")
class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @InjectMocks
    private LivroService livroService;

    private LivroRequest livroRequest;
    private Livro livroSalvo;
    private static final String ID = "abc123";
    private static final String ISBN = "978-0132350884";

    @BeforeEach
    void setUp() {
        livroRequest = new LivroRequest(
                "Clean Code",
                "Robert C. Martin",
                ISBN,
                2008,
                GeneroPojo.TECNOLOGIA,
                true
        );

        livroSalvo = Livro.builder()
                .id(ID)
                .titulo("Clean Code")
                .autor("Robert C. Martin")
                .isbn(ISBN)
                .anoPublicacao(2008)
                .genero(GeneroPojo.TECNOLOGIA)
                .disponivel(true)
                .dataInclusao(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("deve criar livro com sucesso")
        void criar_comSucesso() {
            when(livroRepository.existsByIsbn(ISBN)).thenReturn(false);
            when(livroRepository.save(any(Livro.class))).thenReturn(livroSalvo);

            LivroResponse response = livroService.criar(livroRequest);

            assertThat(response).isNotNull();
            assertThat(response.titulo()).isEqualTo("Clean Code");
            assertThat(response.isbn()).isEqualTo(ISBN);
            assertThat(response.id()).isEqualTo(ID);
            verify(livroRepository).save(any(Livro.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando ISBN já cadastrado")
        void criar_isbnDuplicado_lancaException() {
            when(livroRepository.existsByIsbn(ISBN)).thenReturn(true);

            assertThatThrownBy(() -> livroService.criar(livroRequest))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("já está cadastrado")
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("ISBN_DUPLICADO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando ano de publicação é futuro")
        void criar_anoFuturo_lancaException() {
            LivroRequest requestInvalido = new LivroRequest(
                    "Livro Futuro", "Autor", "111-222", Year.now().getValue() + 1,
                    GeneroPojo.FANTASIA, true
            );

            assertThatThrownBy(() -> livroService.criar(requestInvalido))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("ANO_PUBLICACAO_INVALIDO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

            verify(livroRepository, never()).existsByIsbn(any());
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar livro quando encontrado")
        void buscarPorId_comSucesso() {
            when(livroRepository.findById(ID)).thenReturn(Optional.of(livroSalvo));

            LivroResponse response = livroService.buscarPorId(ID);

            assertThat(response.id()).isEqualTo(ID);
            assertThat(response.titulo()).isEqualTo("Clean Code");
        }

        @Test
        @DisplayName("deve lançar exceção quando livro não encontrado")
        void buscarPorId_naoEncontrado_lancaException() {
            when(livroRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> livroService.buscarPorId("id-invalido"))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("não encontrado")
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("LIVRO_NAO_ENCONTRADO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("deve listar livros sem filtro com paginação")
        void listar_semFiltro_comSucesso() {
            Page<Livro> page = new PageImpl<>(List.of(livroSalvo));
            when(livroRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<LivroResponse> result = livroService.listar(0, 10, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).titulo()).isEqualTo("Clean Code");
        }

        @Test
        @DisplayName("deve listar livros com filtro por gênero")
        void listar_comFiltroGenero_comSucesso() {
            Page<Livro> page = new PageImpl<>(List.of(livroSalvo));
            when(livroRepository.findByGenero(eq(GeneroPojo.TECNOLOGIA), any(Pageable.class))).thenReturn(page);

            Page<LivroResponse> result = livroService.listar(0, 10, GeneroPojo.TECNOLOGIA);

            assertThat(result.getContent()).hasSize(1);
            verify(livroRepository).findByGenero(eq(GeneroPojo.TECNOLOGIA), any(Pageable.class));
        }

        @Test
        @DisplayName("deve retornar página vazia quando não há livros")
        void listar_semLivros_retornaPaginaVazia() {
            when(livroRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            Page<LivroResponse> result = livroService.listar(0, 10, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("deve atualizar livro com sucesso")
        void atualizar_comSucesso() {
            when(livroRepository.findById(ID)).thenReturn(Optional.of(livroSalvo));
            when(livroRepository.existsByIsbnAndIdNot(ISBN, ID)).thenReturn(false);
            when(livroRepository.save(any(Livro.class))).thenReturn(livroSalvo);

            LivroResponse response = livroService.atualizar(ID, livroRequest);

            assertThat(response).isNotNull();
            verify(livroRepository).save(any(Livro.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando livro não encontrado")
        void atualizar_naoEncontrado_lancaException() {
            when(livroRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> livroService.atualizar("id-invalido", livroRequest))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("LIVRO_NAO_ENCONTRADO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("deve lançar exceção quando ISBN pertence a outro livro")
        void atualizar_isbnDuplicado_lancaException() {
            when(livroRepository.findById(ID)).thenReturn(Optional.of(livroSalvo));
            when(livroRepository.existsByIsbnAndIdNot(ISBN, ID)).thenReturn(true);

            assertThatThrownBy(() -> livroService.atualizar(ID, livroRequest))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("ISBN_DUPLICADO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando ano de publicação é futuro")
        void atualizar_anoFuturo_lancaException() {
            LivroRequest requestInvalido = new LivroRequest(
                    "Título", "Autor", ISBN, Year.now().getValue() + 1, GeneroPojo.FANTASIA, true
            );

            assertThatThrownBy(() -> livroService.atualizar(ID, requestInvalido))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("ANO_PUBLICACAO_INVALIDO"));
        }
    }

    @Nested
    @DisplayName("deletar()")
    class Deletar {

        @Test
        @DisplayName("deve deletar livro com sucesso")
        void deletar_comSucesso() {
            when(livroRepository.existsById(ID)).thenReturn(true);
            doNothing().when(livroRepository).deleteById(ID);

            assertThatCode(() -> livroService.deletar(ID)).doesNotThrowAnyException();
            verify(livroRepository).deleteById(ID);
        }

        @Test
        @DisplayName("deve lançar exceção quando livro não encontrado")
        void deletar_naoEncontrado_lancaException() {
            when(livroRepository.existsById(any())).thenReturn(false);

            assertThatThrownBy(() -> livroService.deletar("id-invalido"))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("LIVRO_NAO_ENCONTRADO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

            verify(livroRepository, never()).deleteById(any());
        }
    }
}
