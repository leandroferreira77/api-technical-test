package br.com.sicredi.api_technical_test.service;

import br.com.sicredi.api_technical_test.dto.AuthRequest;
import br.com.sicredi.api_technical_test.dto.AuthResponse;
import br.com.sicredi.api_technical_test.exception.NegocioException;
import br.com.sicredi.api_technical_test.model.User;
import br.com.sicredi.api_technical_test.repository.UserRepository;
import br.com.sicredi.api_technical_test.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Testes unitários")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "usuario@teste.com";
    private static final String SENHA = "senha@123";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
    private static final String SENHA_HASH = "$2a$10$AbCdEfGhIjKlMnOpQrStUu";

    @Nested
    @DisplayName("autenticar()")
    class Autenticar {

        @Test
        @DisplayName("deve autenticar com sucesso e retornar token JWT")
        void autenticar_comSucesso_retornaToken() {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(EMAIL)
                    .password(SENHA_HASH)
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn(TOKEN);

            AuthResponse response = authService.autenticar(new AuthRequest(EMAIL, SENHA));

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(TOKEN);
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userDetailsService).loadUserByUsername(EMAIL);
            verify(jwtService).generateToken(userDetails);
        }

        @Test
        @DisplayName("deve propagar BadCredentialsException quando credenciais são inválidas")
        void autenticar_credenciaisInvalidas_propagaException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Email ou senha incorretos"));

            assertThatThrownBy(() -> authService.autenticar(new AuthRequest(EMAIL, SENHA)))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Email ou senha incorretos");

            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(jwtService, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("deve registrar novo usuário com senha encriptada e enabled=1")
        void registrar_comSucesso_salvaSenhaEncriptadaEEnabled() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(SENHA)).thenReturn(SENHA_HASH);

            assertThatCode(() -> authService.registrar(new AuthRequest(EMAIL, SENHA)))
                    .doesNotThrowAnyException();

            verify(userRepository).existsByEmail(EMAIL);
            verify(passwordEncoder).encode(SENHA);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("deve salvar usuário com os campos corretos")
        void registrar_comSucesso_verificaCamposDoUsuarioSalvo() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(SENHA)).thenReturn(SENHA_HASH);

            authService.registrar(new AuthRequest(EMAIL, SENHA));

            verify(userRepository).save(
                    argThat(user ->
                            user.getEmail().equals(EMAIL)
                            && user.getSenha().equals(SENHA_HASH)
                            && Integer.valueOf(1).equals(user.getEnabled())
                            && user.getDataCadastro() != null
                    )
            );
        }

        @Test
        @DisplayName("deve lançar NegocioException quando email já está cadastrado")
        void registrar_emailDuplicado_lancaNegocioException() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.registrar(new AuthRequest(EMAIL, SENHA)))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("já está cadastrado")
                    .satisfies(ex -> assertThat(((NegocioException) ex).getCodigo()).isEqualTo("EMAIL_DUPLICADO"))
                    .satisfies(ex -> assertThat(((NegocioException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }
}
