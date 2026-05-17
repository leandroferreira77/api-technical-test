package br.com.sicredi.api_technical_test.controller;

import br.com.sicredi.api_technical_test.config.SecurityConfig;
import br.com.sicredi.api_technical_test.dto.AuthRequest;
import br.com.sicredi.api_technical_test.dto.AuthResponse;
import br.com.sicredi.api_technical_test.exception.NegocioException;
import br.com.sicredi.api_technical_test.security.JwtService;
import br.com.sicredi.api_technical_test.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController - Testes unitários")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String EMAIL = "usuario@teste.com";
    private static final String SENHA = "senha@123";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.payload.signature";

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("deve retornar 200 e token JWT quando credenciais são válidas")
        void login_comSucesso_retorna200ComToken() throws Exception {
            when(authService.autenticar(any(AuthRequest.class)))
                    .thenReturn(new AuthResponse(TOKEN));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(EMAIL, SENHA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.token", is(TOKEN)));
        }

        @Test
        @DisplayName("deve retornar 401 quando credenciais são inválidas")
        void login_credenciaisInvalidas_retorna401ComCodigo() throws Exception {
            when(authService.autenticar(any(AuthRequest.class)))
                    .thenThrow(new BadCredentialsException("Email ou senha incorretos"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(EMAIL, SENHA))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo", is("CREDENCIAIS_INVALIDAS")))
                    .andExpect(jsonPath("$.mensagem", is("Email ou senha incorretos.")));
        }

        @Test
        @DisplayName("deve retornar 400 quando email tem formato inválido")
        void login_emailInvalido_retorna400() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest("nao-e-email", SENHA))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão vazios")
        void login_camposVazios_retorna400() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest("", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("deve retornar 201 quando registro é bem-sucedido")
        void register_comSucesso_retorna201() throws Exception {
            doNothing().when(authService).registrar(any(AuthRequest.class));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(EMAIL, SENHA))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("deve retornar 409 quando email já está cadastrado")
        void register_emailDuplicado_retorna409ComCodigo() throws Exception {
            doThrow(new NegocioException(
                    "EMAIL_DUPLICADO",
                    "Email '" + EMAIL + "' já está cadastrado.",
                    HttpStatus.CONFLICT))
                    .when(authService).registrar(any(AuthRequest.class));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(EMAIL, SENHA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo", is("EMAIL_DUPLICADO")))
                    .andExpect(jsonPath("$.mensagem", is("Email '" + EMAIL + "' já está cadastrado.")));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão vazios")
        void register_camposVazios_retorna400() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest("", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }

        @Test
        @DisplayName("deve retornar 400 quando email tem formato inválido")
        void register_emailInvalido_retorna400() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest("usuario-sem-arroba", SENHA))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo", is("VALIDACAO_INVALIDA")));
        }
    }
}
