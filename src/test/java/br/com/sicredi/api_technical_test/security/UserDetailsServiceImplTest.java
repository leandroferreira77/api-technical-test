package br.com.sicredi.api_technical_test.security;

import br.com.sicredi.api_technical_test.model.User;
import br.com.sicredi.api_technical_test.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl - Testes unitários")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private static final String EMAIL = "usuario@teste.com";
    private static final String SENHA_HASH = "$2a$10$AbCdEfGhIjKlMnOpQrStUu";

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsername {

        @Test
        @DisplayName("deve retornar UserDetails habilitado quando usuário está ativo (enabled=1)")
        void loadUserByUsername_usuarioAtivo_retornaUserDetailsHabilitado() {
            User usuarioAtivo = User.builder()
                    .id("id-123")
                    .email(EMAIL)
                    .senha(SENHA_HASH)
                    .enabled(1)
                    .dataCadastro(LocalDateTime.now())
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioAtivo));

            UserDetails resultado = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getUsername()).isEqualTo(EMAIL);
            assertThat(resultado.getPassword()).isEqualTo(SENHA_HASH);
            assertThat(resultado.isEnabled()).isTrue();
            verify(userRepository).findByEmail(EMAIL);
        }

        @Test
        @DisplayName("deve retornar UserDetails desabilitado quando usuário está inativo (enabled=0)")
        void loadUserByUsername_usuarioInativo_retornaUserDetailsDesabilitado() {
            User usuarioInativo = User.builder()
                    .id("id-456")
                    .email(EMAIL)
                    .senha(SENHA_HASH)
                    .enabled(0)
                    .dataCadastro(LocalDateTime.now())
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioInativo));

            UserDetails resultado = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(resultado.isEnabled()).isFalse();
            assertThat(resultado.getUsername()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("deve lançar UsernameNotFoundException quando email não existe")
        void loadUserByUsername_emailNaoEncontrado_lancaUsernameNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(EMAIL);

            verify(userRepository).findByEmail(EMAIL);
        }
    }
}
