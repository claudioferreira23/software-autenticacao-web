# autenticacao-api

Backend da aplicação de autenticação de usuários (Spring Boot 4, Java 21, PostgreSQL 16).

## Pré-requisitos

- Java 21
- Docker em execução (usado pelo banco local e pelos testes com Testcontainers)

O Maven não precisa estar instalado: use sempre o wrapper `./mvnw`.

## Configuração

Todos os comandos abaixo são executados dentro da pasta `backend/`.

Crie o arquivo `.env` a partir do exemplo e troque a senha do banco:

```bash
cp .env.example .env
```

| Variável | Uso |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | usuário do banco |
| `DB_PASSWORD` | senha do banco |

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

Para conferir a tabela criada:

```bash
docker compose exec postgres psql -U "$DB_USERNAME" -d autenticacao -c '\d usuarios'
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
