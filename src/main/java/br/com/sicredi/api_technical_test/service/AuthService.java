package br.com.sicredi.api_technical_test.service;

import br.com.sicredi.api_technical_test.dto.AuthRequest;
import br.com.sicredi.api_technical_test.dto.AuthResponse;
import br.com.sicredi.api_technical_test.exception.NegocioException;
import br.com.sicredi.api_technical_test.model.User;
import br.com.sicredi.api_technical_test.repository.UserRepository;
import br.com.sicredi.api_technical_test.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthResponse autenticar(AuthRequest request) {
        log.debug("Início - autenticar: email={}", request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        AuthResponse response = new AuthResponse(jwtService.generateToken(userDetails));

        log.debug("Fim - autenticar: token JWT gerado para email={}", request.email());
        return response;
    }

    public void registrar(AuthRequest request) {
        log.debug("Início - registrar: email={}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Erro - registrar: email='{}' já está cadastrado", request.email());
            throw new NegocioException("EMAIL_DUPLICADO",
                    "Email '" + request.email() + "' já está cadastrado.", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .dataCadastro(LocalDateTime.now())
                .enabled(1)
                .build();

        userRepository.save(user);
        log.debug("Fim - registrar: usuário criado com email={}", request.email());
    }
}
