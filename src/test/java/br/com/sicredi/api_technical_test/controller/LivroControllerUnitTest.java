package br.com.sicredi.api_technical_test.controller;

import br.com.sicredi.api_technical_test.config.SecurityConfig;
import br.com.sicredi.api_technical_test.dto.LivroRequest;
import br.com.sicredi.api_technical_test.dto.LivroResponse;
import br.com.sicredi.api_technical_test.exception.NegocioException;
import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.security.JwtService;
import br.com.sicredi.api_technical_test.service.LivroService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LivroController.class)
@Import(SecurityConfig.class)
@WithMockUser
@DisplayName("LivroController - Testes unitários")
class LivroControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LivroService livroService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String ID = "abc123";
    private static final String ISBN = "978-0132350884";

    private LivroRequest livroValido;
    private LivroResponse livroResponse;

    @BeforeEach
    void setUp() {
        livroValido = new LivroRequest("Clean Code", "Robert C. Martin", ISBN, 2008, GeneroPojo.TECNOLOGIA, true);

        livroResponse = new LivroResponse(
                ID, "Clean Code", "Robert C. Martin", ISBN, 2008,
                GeneroPojo.TECNOLOGIA, true, LocalDateTime.now(), null
        );
    }

    @Nested
    @DisplayName("POST /livros")
    class CriarLivro {

        @Test
        @DisplayName("deve criar livro e retornar 201 com body")
        void criar_comSucesso_retorna201() throws Exception {
            when(livroService.criar(any(LivroRequest.class))).thenReturn(livroResponse);

            mockMvc.perform(post("/livros")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(ID)))
                    .andExpect(jsonPath("$.titulo", is("Clean Code")))
                    .andExpect(jsonPath("$.isbn", is(ISBN)))
                    .andExpect(jsonPath("$.genero", is("TECNOLOGIA")));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void criar_dadosInvalidos_retorna400() throws Exception {
            LivroRequest invalido = new LivroRequest("", "", "", null, null, null);

            mockMvc.perform(post("/livros")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }

        @Test
        @DisplayName("deve retornar 409 quando ISBN já está cadastrado")
        void criar_isbnDuplicado_retorna409() throws Exception {
            when(livroService.criar(any(LivroRequest.class)))
                    .thenThrow(new NegocioException("ISBN_DUPLICADO",
                            "ISBN '" + ISBN + "' já está cadastrado.", HttpStatus.CONFLICT));

            mockMvc.perform(post("/livros")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is("ISBN_DUPLICADO")));
        }

        @Test
        @DisplayName("deve retornar 401 quando requisição não está autenticada")
        void criar_semAutenticacao_retorna401() throws Exception {
            mockMvc.perform(post("/livros")
                            .with(anonymous())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /livros/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar livro e status 200 quando encontrado")
        void buscarPorId_encontrado_retorna200() throws Exception {
            when(livroService.buscarPorId(ID)).thenReturn(livroResponse);

            mockMvc.perform(get("/livros/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ID)))
                    .andExpect(jsonPath("$.titulo", is("Clean Code")))
                    .andExpect(jsonPath("$.dataInclusao", notNullValue()));
        }

        @Test
        @DisplayName("deve retornar 404 quando livro não existe")
        void buscarPorId_naoEncontrado_retorna404() throws Exception {
            when(livroService.buscarPorId(any()))
                    .thenThrow(new NegocioException("LIVRO_NAO_ENCONTRADO",
                            "Livro não encontrado.", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/livros/{id}", "id-inexistente"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is("LIVRO_NAO_ENCONTRADO")));
        }
    }

    @Nested
    @DisplayName("GET /livros")
    class ListarLivros {

        @Test
        @DisplayName("deve retornar página de livros sem filtro")
        void listar_semFiltro_retorna200() throws Exception {
            PageImpl<LivroResponse> page = new PageImpl<>(
                    List.of(livroResponse), PageRequest.of(0, 10), 1
            );
            when(livroService.listar(0, 10, null)).thenReturn(page);

            mockMvc.perform(get("/livros")
                            .param("pagina", "0")
                            .param("tamanho", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("deve retornar livros filtrados por gênero")
        void listar_comFiltroGenero_retorna200() throws Exception {
            PageImpl<LivroResponse> page = new PageImpl<>(
                    List.of(livroResponse), PageRequest.of(0, 10), 1
            );
            when(livroService.listar(eq(0), eq(10), eq(GeneroPojo.TECNOLOGIA))).thenReturn(page);

            mockMvc.perform(get("/livros")
                            .param("pagina", "0")
                            .param("tamanho", "10")
                            .param("genero", "TECNOLOGIA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].genero", is("TECNOLOGIA")));
        }

        @Test
        @DisplayName("deve usar valores padrão de paginação quando não informados")
        void listar_semParametros_usaValoresPadrao() throws Exception {
            when(livroService.listar(0, 10, null)).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/livros"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /livros/{id}")
    class AtualizarLivro {

        @Test
        @DisplayName("deve atualizar livro e retornar 200")
        void atualizar_comSucesso_retorna200() throws Exception {
            LivroResponse atualizado = new LivroResponse(
                    ID, "Clean Code 2nd Ed", "Robert C. Martin", ISBN, 2020,
                    GeneroPojo.TECNOLOGIA, false, LocalDateTime.now(), LocalDateTime.now()
            );
            when(livroService.atualizar(eq(ID), any(LivroRequest.class))).thenReturn(atualizado);

            mockMvc.perform(put("/livros/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo", is("Clean Code 2nd Ed")))
                    .andExpect(jsonPath("$.disponivel", is(false)));
        }

        @Test
        @DisplayName("deve retornar 404 quando livro não existe")
        void atualizar_naoEncontrado_retorna404() throws Exception {
            when(livroService.atualizar(any(), any()))
                    .thenThrow(new NegocioException("LIVRO_NAO_ENCONTRADO",
                            "Livro não encontrado.", HttpStatus.NOT_FOUND));

            mockMvc.perform(put("/livros/{id}", "id-inexistente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is("LIVRO_NAO_ENCONTRADO")));
        }

        @Test
        @DisplayName("deve retornar 409 quando ISBN já pertence a outro livro")
        void atualizar_isbnDuplicado_retorna409() throws Exception {
            when(livroService.atualizar(any(), any()))
                    .thenThrow(new NegocioException("ISBN_DUPLICADO",
                            "ISBN já cadastrado.", HttpStatus.CONFLICT));

            mockMvc.perform(put("/livros/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is("ISBN_DUPLICADO")));
        }

        @Test
        @DisplayName("deve retornar 400 quando dados de atualização são inválidos")
        void atualizar_dadosInvalidos_retorna400() throws Exception {
            LivroRequest invalido = new LivroRequest("", "", "", null, null, null);

            mockMvc.perform(put("/livros/{id}", ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }
    }

    @Nested
    @DisplayName("DELETE /livros/{id}")
    class DeletarLivro {

        @Test
        @DisplayName("deve deletar livro e retornar 204 sem body")
        void deletar_comSucesso_retorna204() throws Exception {
            doNothing().when(livroService).deletar(ID);

            mockMvc.perform(delete("/livros/{id}", ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando livro não existe")
        void deletar_naoEncontrado_retorna404() throws Exception {
            doThrow(new NegocioException("LIVRO_NAO_ENCONTRADO",
                    "Livro não encontrado.", HttpStatus.NOT_FOUND))
                    .when(livroService).deletar(any());

            mockMvc.perform(delete("/livros/{id}", "id-inexistente"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is("LIVRO_NAO_ENCONTRADO")));
        }
    }
}
