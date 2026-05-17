package br.com.sicredi.api_technical_test.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService - Testes unitários")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    // Mesma chave configurada em application.properties
    private static final String SECRET = "bXlTZWNyZXRLZXlGb3JKV1RUb2tlbkdlbmVyYXRpb25JbkJhc2U2NEZvcm1hdA==";
    private static final long EXPIRATION = 86400000L; // 24h
    private static final String EMAIL = "usuario@teste.com";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);

        userDetails = User.builder()
                .username(EMAIL)
                .password("hash")
                .build();
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("deve gerar token não nulo e não vazio")
        void generateToken_deveRetornarTokenNaoVazio() {
            String token = jwtService.generateToken(userDetails);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("deve gerar token com três partes separadas por ponto (formato JWT)")
        void generateToken_deveRetornarTokenComFormatoJwt() {
            String token = jwtService.generateToken(userDetails);

            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("deve embutir o username como subject do token")
        void generateToken_tokenDeveConterUsernameComoSubject() {
            String token = jwtService.generateToken(userDetails);

            assertThat(jwtService.extractUsername(token)).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsername {

        @Test
        @DisplayName("deve extrair username de token válido")
        void extractUsername_tokenValido_retornaUsername() {
            String token = jwtService.generateToken(userDetails);

            assertThat(jwtService.extractUsername(token)).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("deve lançar exceção para token com assinatura inválida")
        void extractUsername_tokenComAssinaturaInvalida_lancaException() {
            String tokenFalsificado = jwtService.generateToken(userDetails) + "adulterado";

            assertThatThrownBy(() -> jwtService.extractUsername(tokenFalsificado))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve lançar exceção para token expirado")
        void extractUsername_tokenExpirado_lancaException() {
            JwtService expiredJwtService = new JwtService();
            ReflectionTestUtils.setField(expiredJwtService, "secret", SECRET);
            ReflectionTestUtils.setField(expiredJwtService, "expiration", -1000L);
            String tokenExpirado = expiredJwtService.generateToken(userDetails);

            assertThatThrownBy(() -> jwtService.extractUsername(tokenExpirado))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve lançar exceção para string que não é um token JWT")
        void extractUsername_textoArbitrario_lancaException() {
            assertThatThrownBy(() -> jwtService.extractUsername("nao.e.um.jwt"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("deve retornar true para token válido do mesmo usuário")
        void isTokenValid_tokenValidoParaUsuario_retornaTrue() {
            String token = jwtService.generateToken(userDetails);

            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando token pertence a outro usuário")
        void isTokenValid_tokenDeOutroUsuario_retornaFalse() {
            UserDetails outroUsuario = User.builder()
                    .username("outro@teste.com")
                    .password("hash")
                    .build();
            String token = jwtService.generateToken(userDetails);

            assertThat(jwtService.isTokenValid(token, outroUsuario)).isFalse();
        }
    }
}
