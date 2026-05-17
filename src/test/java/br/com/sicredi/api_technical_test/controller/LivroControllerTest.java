package br.com.sicredi.api_technical_test.controller;

import br.com.sicredi.api_technical_test.dto.AuthRequest;
import br.com.sicredi.api_technical_test.dto.AuthResponse;
import br.com.sicredi.api_technical_test.dto.LivroRequest;
import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.model.User;
import br.com.sicredi.api_technical_test.repository.LivroRepository;
import br.com.sicredi.api_technical_test.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("LivroController - Testes de integração")
class LivroControllerTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @SuppressWarnings("resource")
	@Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String jwtToken;

    private static final String USER_EMAIL = "test@biblioteca.com";
    private static final String USER_SENHA = "senha@123";
    private static final String ISBN = "978-0132350884";

    @BeforeEach
    void setUp() throws Exception {
        livroRepository.deleteAll();
        userRepository.deleteAll();
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        userRepository.save(User.builder()
                .email(USER_EMAIL)
                .senha(passwordEncoder.encode(USER_SENHA))
                .dataCadastro(LocalDateTime.now())
                .enabled(1)
                .build());

        jwtToken = obterToken(USER_EMAIL, USER_SENHA);
    }

    private String obterToken(String email, String senha) throws Exception {
        String body = objectMapper.writeValueAsString(new AuthRequest(email, senha));
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    private LivroRequest livroValido() {
        return new LivroRequest("Clean Code", "Robert C. Martin", ISBN, 2008, GeneroPojo.TECNOLOGIA, true);
    }

    private String salvarLivroEObterJson(LivroRequest request) throws Exception {
        return mockMvc.perform(post("/livros")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Nested
    @DisplayName("POST /livros")
    class CriarLivro {

        @Test
        @DisplayName("deve criar livro no MongoDB e retornar 201")
        void criar_retorna201() throws Exception {
            mockMvc.perform(post("/livros")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.titulo", is("Clean Code")))
                    .andExpect(jsonPath("$.isbn", is(ISBN)))
                    .andExpect(jsonPath("$.genero", is("TECNOLOGIA")));

            assertThat(livroRepository.existsByIsbn(ISBN)).isTrue();
        }

        @Test
        @DisplayName("deve retornar 400 quando dados inválidos")
        void criar_dadosInvalidos_retorna400() throws Exception {
            LivroRequest invalido = new LivroRequest("", "", "", null, null, null);

            mockMvc.perform(post("/livros")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }

        @Test
        @DisplayName("deve retornar 409 quando ISBN já existe")
        void criar_isbnDuplicado_retorna409() throws Exception {
            salvarLivroEObterJson(livroValido());

            mockMvc.perform(post("/livros")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is("ISBN_DUPLICADO")));
        }

        @Test
        @DisplayName("deve retornar 401 sem token JWT")
        void criar_semToken_retorna401() throws Exception {
            mockMvc.perform(post("/livros")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /livros/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar livro quando encontrado e gravar cache no Redis")
        void buscarPorId_retorna200_eGravaCacheRedis() throws Exception {
            String responseJson = salvarLivroEObterJson(livroValido());
            String id = objectMapper.readTree(responseJson).get("id").asText();

            mockMvc.perform(get("/livros/" + id)
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id)))
                    .andExpect(jsonPath("$.titulo", is("Clean Code")));

            assertThat(stringRedisTemplate.hasKey("biblioteca:livro:" + id)).isTrue();
        }

        @Test
        @DisplayName("deve retornar 404 quando livro não encontrado")
        void buscarPorId_naoEncontrado_retorna404() throws Exception {
            mockMvc.perform(get("/livros/id-inexistente")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is("LIVRO_NAO_ENCONTRADO")));
        }
    }

    @Nested
    @DisplayName("GET /livros")
    class ListarLivros {

        @Test
        @DisplayName("deve listar livros com paginação")
        void listar_retorna200() throws Exception {
            salvarLivroEObterJson(livroValido());
            salvarLivroEObterJson(new LivroRequest("Domain-Driven Design", "Eric Evans", "978-0321125217", 2003,
                    GeneroPojo.TECNOLOGIA, true));

            mockMvc.perform(get("/livros")
                            .header("Authorization", "Bearer " + jwtToken)
                            .param("pagina", "0")
                            .param("tamanho", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("deve filtrar por gênero")
        void listar_comFiltroGenero_retorna200() throws Exception {
            salvarLivroEObterJson(livroValido());
            salvarLivroEObterJson(new LivroRequest("O Hobbit", "J. R. R. Tolkien", "978-0547928227", 1937,
                    GeneroPojo.FANTASIA, true));

            mockMvc.perform(get("/livros")
                            .header("Authorization", "Bearer " + jwtToken)
                            .param("genero", "TECNOLOGIA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].genero", is("TECNOLOGIA")));
        }
    }

    @Nested
    @DisplayName("PUT /livros/{id}")
    class AtualizarLivro {

        @Test
        @DisplayName("deve atualizar livro no MongoDB e retornar 200")
        void atualizar_retorna200() throws Exception {
            String responseJson = salvarLivroEObterJson(livroValido());
            String id = objectMapper.readTree(responseJson).get("id").asText();
            LivroRequest atualizado = new LivroRequest(
                    "Clean Code 2nd Ed", "Robert C. Martin", ISBN, 2020, GeneroPojo.TECNOLOGIA, false
            );

            mockMvc.perform(put("/livros/" + id)
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(atualizado)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo", is("Clean Code 2nd Ed")))
                    .andExpect(jsonPath("$.disponivel", is(false)));

            assertThat(livroRepository.findById(id)).hasValueSatisfying(livro ->
                    assertThat(livro.getTitulo()).isEqualTo("Clean Code 2nd Ed"));
        }

        @Test
        @DisplayName("deve retornar 400 quando atualização possui dados inválidos")
        void atualizar_dadosInvalidos_retorna400() throws Exception {
            String responseJson = salvarLivroEObterJson(livroValido());
            String id = objectMapper.readTree(responseJson).get("id").asText();
            LivroRequest invalido = new LivroRequest("", "", "", null, null, null);

            mockMvc.perform(put("/livros/" + id)
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }

        @Test
        @DisplayName("deve retornar 404 quando livro não existe")
        void atualizar_naoEncontrado_retorna404() throws Exception {
            mockMvc.perform(put("/livros/id-inexistente")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(livroValido())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is("LIVRO_NAO_ENCONTRADO")));
        }
    }

    @Nested
    @DisplayName("DELETE /livros/{id}")
    class DeletarLivro {

        @Test
        @DisplayName("deve deletar livro do MongoDB e retornar 204")
        void deletar_retorna204() throws Exception {
            String responseJson = salvarLivroEObterJson(livroValido());
            String id = objectMapper.readTree(responseJson).get("id").asText();

            mockMvc.perform(delete("/livros/" + id)
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isNoContent());

            assertThat(livroRepository.existsById(id)).isFalse();
        }

        @Test
        @DisplayName("deve retornar 404 quando livro não existe")
        void deletar_naoEncontrado_retorna404() throws Exception {
            mockMvc.perform(delete("/livros/id-inexistente")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo", is("LIVRO_NAO_ENCONTRADO")));
        }
    }
}
