# autenticacao-api

Backend da aplicação de autenticação de usuários (Spring Boot 4, Java 21, PostgreSQL 16).

## Pré-requisitos

- Java 21
- Docker em execução (usado pelo banco local e pelos testes com Testcontainers)

O Maven não precisa estar instalado: use sempre o wrapper `./mvnw`.

## Configuração

Todos os comandos abaixo são executados dentro da pasta `backend/`.

Crie o arquivo `.env` a partir do exemplo e troque as senhas:

```bash
cp .env.example .env
```

| Variável | Uso |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | usuário do banco |
| `DB_PASSWORD` | senha do banco |
| `SEED_SENHA_PADRAO` | senha dos usuários de exemplo, usada só no perfil `dev` |

O `.env` é ignorado pelo git e nunca deve ser commitado.

## Subir o banco

```bash
docker compose up -d
```

O container sobe o PostgreSQL 16 na porta `5432`, com o banco `autenticacao`. Os dados ficam no volume `postgres-data`.

## Executar a aplicação

```bash
set -a && . ./.env && set +a
./mvnw spring-boot:run
```

Ao iniciar, o Flyway aplica as migrações de `src/main/resources/db/migration`. A API fica disponível em `http://localhost:8080`.

Para subir com os usuários de exemplo, use o perfil `dev`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

No perfil `dev`, a aplicação cria os usuários abaixo se eles ainda não existirem, todos com a senha definida em `SEED_SENHA_PADRAO`. Eles servem apenas para desenvolvimento e testes manuais.

| Nome | E-mail |
|---|---|
| Maria Exemplo | `maria@exemplo.com` |
| João Exemplo | `joao@exemplo.com` |

Para conferir a tabela criada:

```bash
docker compose exec postgres psql -U "$DB_USERNAME" -d autenticacao -c '\d usuarios'
```

## Endpoint de login

`POST /api/auth/login`, com corpo JSON `{ "email": "...", "senha": "..." }`.

| Status | Significado |
|---|---|
| 200 | autenticado; o corpo traz `id`, `nome` e `email` do usuário |
| 400 | entrada inválida (`ENTRADA_INVALIDA`) |
| 401 | e-mail ou senha inválidos (`CREDENCIAIS_INVALIDAS`) |
| 500 | erro interno (`ERRO_INTERNO`) |

Exemplo com um usuário do perfil `dev`:

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"maria@exemplo.com\",\"senha\":\"$SEED_SENHA_PADRAO\"}"
```

## Executar os testes

```bash
./mvnw -B verify
```

Os testes sobem um PostgreSQL próprio via Testcontainers e não dependem do banco do `compose.yaml` nem do `.env`. Na primeira execução, a imagem `postgres:16-alpine` é baixada.

## Parar o banco

```bash
docker compose down
```

Os dados do volume são mantidos. Para apagá-los, use `docker compose down -v`.
