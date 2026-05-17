package br.com.sicredi.api_technical_test.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter - Testes unitários")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String TOKEN = "header.payload.signature";
    private static final String EMAIL = "usuario@teste.com";
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        userDetails = User.builder()
                .username(EMAIL)
                .password("hash")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Requisições sem token JWT")
    class SemToken {

        @Test
        @DisplayName("deve passar para o próximo filtro quando não há header Authorization")
        void doFilter_semHeaderAuthorization_passaParaProximoFiltro() throws Exception {
            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("deve passar para o próximo filtro quando header não começa com 'Bearer '")
        void doFilter_headerSemPrefixoBearer_passaParaProximoFiltro() throws Exception {
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Requisições com token JWT")
    class ComToken {

        @Test
        @DisplayName("deve definir autenticação no contexto quando token é válido")
        void doFilter_comTokenValido_defineAutenticacaoNoContexto() throws Exception {
            request.addHeader("Authorization", "Bearer " + TOKEN);

            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(EMAIL);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve passar para o próximo filtro sem autenticar quando extractUsername lança exceção")
        void doFilter_extractUsernameThrows_passaParaProximoFiltroSemAutenticar() throws Exception {
            request.addHeader("Authorization", "Bearer token.invalido");

            when(jwtService.extractUsername("token.invalido"))
                    .thenThrow(new RuntimeException("Token malformado"));

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("deve passar para o próximo filtro sem autenticar quando token é inválido para o usuário")
        void doFilter_tokenInvalidoParaUsuario_naoDefineAutenticacao() throws Exception {
            request.addHeader("Authorization", "Bearer " + TOKEN);

            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve ignorar token quando usuário já possui autenticação no contexto")
        void doFilter_usuarioJaAutenticado_naoReprocessaAutenticacao() throws Exception {
            UsernamePasswordAuthenticationToken authExistente =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authExistente);

            request.addHeader("Authorization", "Bearer " + TOKEN);
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);

            filter.doFilter(request, response, filterChain);

            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(jwtService, never()).isTokenValid(any(), any());
            verify(filterChain).doFilter(request, response);
        }
    }
}
