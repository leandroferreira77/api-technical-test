package br.com.sicredi.api_technical_test.controller;

import br.com.sicredi.api_technical_test.dto.LivroRequest;
import br.com.sicredi.api_technical_test.dto.LivroResponse;
import br.com.sicredi.api_technical_test.model.GeneroPojo;
import br.com.sicredi.api_technical_test.service.LivroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/livros")
@RequiredArgsConstructor
@Tag(name = "Livros", description = "CRUD de livros da biblioteca")
@SecurityRequirement(name = "bearerAuth")
public class LivroController {

    private final LivroService livroService;

    @PostMapping
    @Operation(summary = "Cadastrar novo livro")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livro criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "ISBN já cadastrado")
    })
    public ResponseEntity<LivroResponse> criar(@RequestBody @Valid LivroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(livroService.criar(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar livro por ID (com cache Redis)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livro encontrado"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<LivroResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(livroService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar livros com paginação e filtro opcional por gênero")
    @ApiResponse(responseCode = "200", description = "Lista paginada de livros")
    public ResponseEntity<Page<LivroResponse>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(required = false) GeneroPojo genero
    ) {
        return ResponseEntity.ok(livroService.listar(pagina, tamanho, genero));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar livro (invalida cache)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livro atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "409", description = "ISBN já cadastrado")
    })
    public ResponseEntity<LivroResponse> atualizar(
            @PathVariable String id,
            @RequestBody @Valid LivroRequest request
    ) {
        return ResponseEntity.ok(livroService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover livro (invalida cache)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Livro removido"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        livroService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
