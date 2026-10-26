# Portal DS API

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
| `SESSION_COOKIE_SECURE` | `false` no HTTP local; no perfil `prod` o padrão é `true` |
| `CORS_ALLOWED_ORIGINS` | origens autorizadas a chamar a API via CORS com credenciais (padrão `http://localhost:4200`). **Obrigatório** fora do ambiente local. |

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

A documentação interativa Swagger fica disponível em `http://localhost:8080/swagger-ui.html`.
Ao acessar a raiz `http://localhost:8080/`, o navegador é redirecionado para ela.

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

## Autenticação e segurança

A autenticação usa sessão HTTP mantida no servidor. O navegador recebe apenas o cookie
`JSESSIONID`, configurado como `HttpOnly` e `SameSite=Lax`; nenhuma senha ou token de
autenticação precisa ser armazenado no JavaScript. O identificador da sessão é trocado
no login e invalidado no logout.

As operações que alteram estado exigem CSRF. Antes delas, chame `GET /api/auth/csrf`,
mantenha o token retornado apenas em memória e envie-o no cabeçalho `X-XSRF-TOKEN`.
O token também fica associado à sessão no servidor. A sessão expira após 30 minutos por
padrão (`SESSION_TIMEOUT` pode alterar esse valor).

Como o cookie é `SameSite=Lax`, o navegador só o envia em requisições do mesmo "site"
(mesmo domínio registrável; subdomínios contam). Em produção, o frontend e esta API
precisam ficar em domínios registráveis iguais (ex.: subdomínios de `portal-ds.com`) ou
atrás do mesmo proxy reverso. Se precisarem ficar em domínios totalmente diferentes, o
cookie teria que mudar para `SameSite=None` + `Secure`, o que não está configurado hoje.

O endpoint `POST /api/auth/login` também limita tentativas malsucedidas por IP (20) e por
e-mail (5) em janelas de 1 minuto, devolvendo `429` com o cabeçalho `Retry-After` quando o
limite é atingido.

## Endpoints

### Cadastro

`POST /api/auth/cadastro`

```json
{ "nome": "Maria", "email": "maria@exemplo.com", "senha": "Senha-Correta-123" }
```

Retorna `201` com `{ "id", "nome", "email" }`. Nome e e-mail são normalizados, a senha
deve ter entre 8 e 128 caracteres e é persistida somente como hash Argon2id. Um e-mail
já cadastrado retorna `409` com o código `EMAIL_JA_CADASTRADO`.

### Login

`POST /api/auth/login`

```json
{ "email": "maria@exemplo.com", "senha": "Senha-Correta-123" }
```

| Status | Significado |
|---|---|
| 200 | sessão criada; o corpo traz `id`, `nome` e `email` do usuário |
| 400 | entrada inválida (`ENTRADA_INVALIDA`) |
| 401 | e-mail ou senha inválidos (`CREDENCIAIS_INVALIDAS`) |
| 429 | muitas tentativas; aguarde o tempo do cabeçalho `Retry-After` (`TENTATIVAS_EXCEDIDAS`) |
| 500 | erro interno (`ERRO_INTERNO`) |

### Usuário autenticado

`GET /api/auth/me` retorna a identidade da sessão atual. Sem uma sessão válida, retorna
`401` com `NAO_AUTENTICADO`.

### Logout

`POST /api/auth/logout` invalida a sessão e retorna `204`. A requisição também exige o
cabeçalho CSRF.

Exemplo completo com `curl` e um usuário do perfil `dev`:

```bash
CSRF_TOKEN=$(curl -s -c cookies.txt http://localhost:8080/api/auth/csrf | jq -r .token)

curl -i -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d "{\"email\":\"maria@exemplo.com\",\"senha\":\"$SEED_SENHA_PADRAO\"}"

curl -b cookies.txt http://localhost:8080/api/auth/me
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
