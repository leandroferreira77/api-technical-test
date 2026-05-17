# Biblioteca API

Microsserviço RESTfull de gerenciamento de acervo de livros, desenvolvido como prova técnica usando boas práticas de engenharia de software, arquitetura limpa e ecossistema Spring Boot moderno.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Stack Tecnológica](#stack-tecnológica)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Subindo os Containers](#subindo-os-containers)
- [Executando a API](#executando-a-api)
- [Autenticação JWT](#autenticação-jwt)
- [Endpoints](#endpoints)
- [Cache Redis](#cache-redis)
- [Documentação Swagger](#documentação-swagger)
- [Testes](#testes)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Variáveis de Configuração](#variáveis-de-configuração)

---

## Visão Geral

A API permite o cadastro, consulta, atualização e remoção de livros com:

- Persistência em **MongoDB**
- Cache de leitura com **Redis** (TTL de 10 minutos)
- Autenticação stateless via **JWT** (Spring Security 6)
- Documentação interativa via **Swagger UI**
- Testes unitários e de integração com **Testcontainers** (MongoDB e Redis reais)

---

## Stack Tecnológica

| Tecnologia             | Versão       | Finalidade                                      |
|------------------------|-------------|------------------------------------------------|
| Java                   | 21 (LTS)    | Linguagem principal; uso de `record`, `sealed` |
| Spring Boot            | 3.4.5       | Framework base                                  |
| Spring Data MongoDB    | via Boot    | Persistência NoSQL                              |
| Spring Data Redis      | via Boot    | Abstração de cache                              |
| Spring Security 6      | via Boot    | Autenticação e autorização                      |
| Spring Cache           | via Boot    | Abstração de cache declarativo (`@Cacheable`)   |
| JJWT                   | 0.12.6      | Geração e validação de tokens JWT (HS256)       |
| SpringDoc OpenAPI      | 2.8.8       | Documentação Swagger UI automática              |
| ModelMapper            | 3.2.3       | Mapeamento entre entidades e DTOs               |
| Lombok                 | via Boot    | Redução de boilerplate                          |
| JUnit 5                | via Boot    | Framework de testes                             |
| Mockito                | via Boot    | Mock em testes unitários                        |
| Testcontainers         | via Boot    | Infraestrutura real em testes de integração     |
| MongoDB                | 7.0         | Banco de dados de documentos                    |
| Redis                  | 7.2         | Cache em memória                                |
| Maven                  | 3.9+        | Gerenciamento de build e dependências           |
| Docker / Compose       | -           | Orquestração de containers locais               |

---

## Arquitetura

O projeto segue o padrão **Clean Architecture simplificado**, com separação clara de responsabilidades por camada:

```
┌──────────────────────────────────────────────────────────────┐
│                        Controller                             │
│         (orquestra requisições HTTP, sem regras)             │
└───────────────────────────┬──────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│                         Service                               │
│    (regras de negócio, validações, integração com cache)     │
└──────────┬────────────────────────────────────┬──────────────┘
           │                                    │
┌──────────▼──────────┐             ┌───────────▼──────────────┐
│     Repository       │             │       Redis Cache         │
│  (Spring Data Mongo) │             │  (@Cacheable / @CacheEvict│
└──────────┬──────────┘             └──────────────────────────┘
           │
┌──────────▼──────────┐
│       MongoDB        │
└─────────────────────┘
```

### Princípios aplicados

- **Single Responsibility** — cada classe tem uma única razão para mudar
- **Dependency Inversion** — camadas superiores dependem de abstrações (interfaces), não de implementações
- **DTOs como `record`** — imutáveis por natureza, sem boilerplate; alinhados com Java 21
- **Tratamento de erros centralizado** — `@RestControllerAdvice` garante respostas padronizadas em toda a API
- **Segurança stateless** — nenhuma sessão HTTP é mantida no servidor; autenticação 100% via JWT

---

## Pré-requisitos

- **JDK 21** instalado e configurado no `PATH`
- **Maven 3.9+** (ou usar o wrapper `./mvnw` incluso)
- **Docker** e **Docker Compose** instalados

Verificação rápida:

```bash
java -version    # deve exibir openjdk 21.x
mvn -version     # deve exibir Apache Maven 3.9.x
docker version   # deve exibir Engine e Client
```

---

## Subindo os Containers

O arquivo `docker-compose.yml` na raiz do projeto provisiona MongoDB e Redis:

```bash
# Iniciar em background
docker-compose up -d

# Verificar status
docker-compose ps

# Acompanhar logs
docker-compose logs -f

# Parar e remover containers (mantém volumes)
docker-compose down

# Parar e remover containers + volumes (limpa dados)
docker-compose down -v
```

Após o comando, os serviços estarão disponíveis em:

| Serviço | Host        | Porta |
|---------|-------------|-------|
| MongoDB | `localhost` | 27017 |
| Redis   | `localhost` | 6379  |

---

## Executando a API

### Via Maven Wrapper (recomendado)

```bash
./mvnw spring-boot:run
```

### Via JAR gerado

```bash
# 1. Build do projeto (pula testes)
./mvnw clean package -DskipTests

# 2. Execução
java -jar target/api-technical-test-0.0.1-SNAPSHOT.jar
```

### Com profile específico

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A aplicação sobe na porta padrão **8080**. Confirme com:

```bash
curl -s http://localhost:8080/v3/api-docs | jq '.info.title'
# "Biblioteca API"
```

---

## Autenticação JWT

Todos os endpoints de livros exigem autenticação. O fluxo é:

### 1. Registrar usuário

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@biblioteca.com", "senha": "minhasenha"}'
```

Retorna `201 Created`.

### 2. Obter token JWT

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@biblioteca.com", "senha": "minhasenha"}'
```

Resposta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQ..."
}
```

### 3. Usar o token nas requisições

Inclua o header `Authorization` em todas as chamadas protegidas:

```bash
curl -X GET http://localhost:8080/livros \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

> O token expira em **24 horas** (configurável via `jwt.expiration`).

---

## Endpoints

Base URL: `http://localhost:8080`

### Autenticação

| Método | Endpoint         | Descrição                   | Auth |
|--------|------------------|-----------------------------|------|
| POST   | `/auth/register` | Registrar novo usuário      | Não  |
| POST   | `/auth/login`    | Autenticar e obter token JWT| Não  |

### Livros

| Método | Endpoint       | Descrição                            | Auth |
|--------|----------------|--------------------------------------|------|
| POST   | `/livros`      | Cadastrar novo livro                 | Sim  |
| GET    | `/livros`      | Listar livros com paginação e filtro | Sim  |
| GET    | `/livros/{id}` | Buscar livro por ID (com cache)      | Sim  |
| PUT    | `/livros/{id}` | Atualizar livro (invalida cache)     | Sim  |
| DELETE | `/livros/{id}` | Remover livro (invalida cache)       | Sim  |

### Parâmetros de listagem (`GET /livros`)

| Parâmetro | Tipo        | Padrão | Descrição                    |
|-----------|-------------|--------|------------------------------|
| `pagina`  | `int`       | `0`    | Número da página (base zero) |
| `tamanho` | `int`       | `10`   | Itens por página             |
| `genero`  | `GeneroPojo`| -      | Filtro opcional por gênero   |

**Gêneros disponíveis:** `FICCAO_CIENTIFICA`, `FANTASIA`, `ROMANCE`, `TERROR`, `BIOGRAFIA`, `HISTORIA`, `TECNOLOGIA`, `INFANTIL`

### Exemplo de payload — criar livro

```bash
curl -X POST http://localhost:8080/livros \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Clean Code",
    "autor": "Robert C. Martin",
    "isbn": "978-0132350884",
    "anoPublicacao": 2008,
    "genero": "TECNOLOGIA",
    "disponivel": true
  }'
```

### Formato de erro padronizado

Todos os erros retornam o mesmo padrão:

```json
{
  "codigo": "LIVRO_NAO_ENCONTRADO",
  "mensagem": "Livro com id '63f1a...' não encontrado.",
  "timestamp": "2025-04-27T14:30:00"
}
```

| Código HTTP | Código de negócio     | Situação                                |
|-------------|----------------------|-----------------------------------------|
| 400         | `VALIDACAO_INVALIDA` | Campos obrigatórios ausentes ou inválidos|
| 400         | `ANO_PUBLICACAO_INVALIDO` | Ano maior que o corrente           |
| 401         | `CREDENCIAIS_INVALIDAS` | Email ou senha incorretos            |
| 404         | `LIVRO_NAO_ENCONTRADO` | Livro inexistente no banco            |
| 409         | `ISBN_DUPLICADO`     | ISBN já cadastrado                      |
| 409         | `EMAIL_DUPLICADO`    | Email já cadastrado                     |

---

## Cache Redis

A estratégia de cache é aplicada na camada **Service**, usando a abstração declarativa do Spring Cache:

| Operação          | Anotação       | Comportamento                              |
|-------------------|---------------|--------------------------------------------|
| `buscarPorId(id)` | `@Cacheable`  | Lê do Redis; vai ao MongoDB só no miss     |
| `atualizar(id)`   | `@CacheEvict` | Remove a entrada do Redis ao persistir     |
| `deletar(id)`     | `@CacheEvict` | Remove a entrada do Redis ao excluir       |

**Formato da chave no Redis:** `biblioteca:livro:{id}`  
**TTL:** 10 minutos (configurado via `RedisCacheConfig`)

Inspecionando o cache em tempo real:

```bash
# Conectar no Redis do container
docker exec -it biblioteca-redis redis-cli

# Listar todas as chaves de livros
KEYS biblioteca:livro:*

# Ver TTL de uma entrada
TTL biblioteca:livro:63f1a2b3c4d5e6f7a8b9c0d1

# Ver conteúdo de uma entrada
GET biblioteca:livro:63f1a2b3c4d5e6f7a8b9c0d1
```

---

## Documentação Swagger

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

A UI permite:
- Visualizar todos os endpoints com descrição, parâmetros e exemplos de resposta
- Autenticar via JWT (botão **Authorize** → inserir `Bearer <token>`)
- Executar chamadas diretamente pelo browser

JSON da especificação OpenAPI:

```
http://localhost:8080/v3/api-docs
```

---

## Testes

### Testes unitários (Service layer)

Testam as regras de negócio com repositórios **mockados via Mockito**, sem necessidade de infraestrutura:

```bash
./mvnw test -Dtest="LivroServiceTest"
```

Cenários cobertos:

- Criar livro com sucesso
- Rejeitar ISBN duplicado (409)
- Rejeitar ano de publicação futuro (400)
- Buscar livro existente / inexistente (404)
- Listar com e sem filtro por gênero
- Atualizar livro existente / inexistente
- Atualizar com ISBN de outro livro (409)
- Deletar livro existente / inexistente

### Testes de integração (Controller layer)

Sobem **MongoDB e Redis reais via Testcontainers** e testam o fluxo completo HTTP → Service → Banco, incluindo autenticação JWT:

```bash
./mvnw test -Dtest="LivroControllerIT"
```

Cenários cobertos:

- `POST /livros` → 201, 400, 409
- `GET /livros/{id}` → 200, 404
- `GET /livros` → paginação, filtro por gênero
- `PUT /livros/{id}` → 200, 404
- `DELETE /livros/{id}` → 204, 404
- Requisição sem token JWT → 401

### Executar toda a suíte

```bash
./mvnw verify
```

> Os testes de integração requerem o **Docker rodando** (Testcontainers provisiona os containers automaticamente; não é necessário subir o `docker-compose` antes).

---

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/br/com/sicredi/api_technical_test/
│   │   ├── ApiTechnicalTestApplication.java   # Entrypoint
│   │   ├── config/
│   │   │   ├── ModelMapperConfig.java         # Bean ModelMapper
│   │   │   ├── OpenApiConfig.java             # Configuração Swagger + JWT scheme
│   │   │   ├── RedisCacheConfig.java          # TTL, prefixo de chave, serialização
│   │   │   └── SecurityConfig.java            # Filter chain, endpoints públicos, STATELESS
│   │   ├── controller/
│   │   │   ├── AuthController.java            # POST /auth/login, /auth/register
│   │   │   └── LivroController.java           # CRUD /livros
│   │   ├── dto/                               # Records Java 21 (imutáveis)
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── LivroRequest.java              # Com anotações de validação
│   │   │   └── LivroResponse.java             # Com factory method from(Livro)
│   │   ├── exception/
│   │   │   ├── ApiError.java                  # Envelope de erro (record)
│   │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice centralizado
│   │   │   └── NegocioException.java          # Exceção de negócio com HttpStatus
│   │   ├── model/
│   │   │   ├── GeneroPojo.java                # Enum de gêneros literários
│   │   │   ├── Livro.java                     # @Document MongoDB
│   │   │   └── User.java                      # @Document MongoDB (id, email, senha hash)
│   │   ├── repository/
│   │   │   ├── LivroRepository.java           # MongoRepository + queries derivadas
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java   # OncePerRequestFilter — extrai e valida JWT
│   │   │   ├── JwtService.java                # Gera e valida tokens (JJWT 0.12.x)
│   │   │   └── UserDetailsServiceImpl.java    # Carrega User do MongoDB para o Spring Security
│   │   └── service/
│   │       ├── AuthService.java               # Login e registro
│   │       └── LivroService.java              # CRUD + cache Redis
│   └── resources/
│       └── application.properties
└── test/
    └── java/br/com/sicredi/api_technical_test/
        ├── ApiTechnicalTestApplicationTests.java   # Context load test
        ├── TestcontainersConfiguration.java        # Config MongoDB para dev local
        ├── TestApiTechnicalTestApplication.java    # Bootstrap dev com containers
        ├── controller/
        │   └── LivroControllerIT.java              # Testes de integração
        └── service/
            └── LivroServiceTest.java               # Testes unitários
```

---

## Variáveis de Configuração

Todas as configurações ficam em `src/main/resources/application.properties`:

| Propriedade                          | Valor padrão                          | Descrição                        |
|--------------------------------------|---------------------------------------|----------------------------------|
| `spring.data.mongodb.uri`            | `mongodb://localhost:27017/biblioteca`| URI de conexão com o MongoDB     |
| `spring.data.mongodb.auto-index-creation` | `true`                           | Cria índices automaticamente     |
| `spring.data.redis.host`             | `localhost`                           | Host do Redis                    |
| `spring.data.redis.port`             | `6379`                                | Porta do Redis                   |
| `spring.cache.type`                  | `redis`                               | Provider de cache                |
| `jwt.secret`                         | *(Base64, 256+ bits)*                 | Chave de assinatura HS256        |
| `jwt.expiration`                     | `86400000` (24h em ms)                | TTL do token JWT em milissegundos|
| `springdoc.swagger-ui.path`          | `/swagger-ui.html`                    | URL do Swagger UI                |
| `logging.level.br.com.sicredi`       | `DEBUG`                               | Nível de log da aplicação        |

> **Importante:** em ambientes de produção, o `jwt.secret` deve ser injetado via variável de ambiente ou secret manager (ex: AWS Secrets Manager, Vault), **nunca** versionado em repositório.

---

## Decisões Técnicas Relevantes

**Por que MongoDB?**  
Flexibilidade de schema para o domínio de livros, cujos atributos podem evoluir sem migrations custosas. O MongoDB também oferece indexação nativa em campos como `isbn` e `email`, garantindo unicidade com performance.

**Por que Redis como cache e não cache em memória local?**  
Em uma arquitetura de microsserviços com múltiplas instâncias, caches locais (como Caffeine) geram inconsistência entre pods. O Redis centraliza o estado do cache e é compartilhado entre todas as réplicas.

**Por que JWT stateless?**  
Elimina a necessidade de armazenar sessões no servidor, favorecendo a escalabilidade horizontal. Qualquer instância da API valida o token de forma autônoma, sem consulta a banco ou cache de sessão.

**Por que `record` para DTOs?**  
Records Java 21 são imutáveis por padrão, eliminam getters/setters/equals/hashCode e comunicam claramente que aquele objeto é um portador de dados sem comportamento. Ideal para a camada de transporte.

**Por que Testcontainers em vez de mocks de infraestrutura?**  
Mocks de repositório podem mascarar incompatibilidades entre a query derivada do Spring Data e a versão real do banco. Testcontainers garante que os testes de integração rodam contra a mesma versão de MongoDB e Redis usada em produção.
