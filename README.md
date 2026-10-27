# software-autenticacao-web

Software Web para Autenticação de Usuários (Portal DS), desenvolvido como Atividade Prática Supervisionada (APS) da disciplina **Domínios de Software**, semestre 2026/2, do Bacharelado em Engenharia de Software da Universidade Federal de Goiás (UFG). Docente: Profa. Adriana Silveira de Souza.

**Equipe:** Claudio Cristiano Louza Filho, Claudio Eliziario Silva Ferreira e Pedro Augusto Rodrigues.

## O que a aplicação faz

A aplicação recebe e-mail e senha, verifica se correspondem a um usuário cadastrado e, em caso de sucesso, devolve a identidade desse usuário (identificador, nome e e-mail) e mantém uma sessão autenticada. Em caso de falha, devolve um erro de autenticação, sem indicar se o e-mail existe ou se a senha está errada.

Para que o fluxo possa ser exercitado de ponta a ponta, a aplicação também oferece cadastro de usuários, consulta do usuário da sessão, encerramento da sessão (logout), proteção CSRF e limite de tentativas de login.

## Arquitetura

| Camada | Tecnologia | Porta |
|---|---|---|
| Apresentação | Angular 22 (SPA executada no navegador) | — |
| Servidor Web (desenvolvimento) | Angular CLI (`ng serve`), com proxy de `/api` para o backend | 4200 |
| Servidor de aplicação | Spring Boot 4.1 com Tomcat embutido e Spring Security, Java 21 | 8080 |
| Repositório de dados | PostgreSQL 16 em contêiner Docker | 5432 |

O navegador acessa apenas `http://localhost:4200`: o `ng serve` entrega a interface e encaminha as chamadas a `/api` para o Spring Boot (`frontend/proxy.conf.json`). A identidade autenticada fica em uma sessão HTTP no servidor; o navegador recebe somente o cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`). As operações que alteram estado exigem o token CSRF obtido em `GET /api/auth/csrf`, enviado no cabeçalho `X-XSRF-TOKEN`. As senhas são armazenadas somente como hash Argon2id.

As fontes dos diagramas do projeto (componentes, implantação, classes, atividades e sequência) e os comandos para gerar as imagens estão em [`docs/diagramas`](docs/diagramas).

## Estrutura do repositório

```
backend/        API Spring Boot (detalhes em backend/README.md)
frontend/       Interface Angular (detalhes em frontend/README.md)
docs/diagramas/ Fontes (.dot e .mmd) dos diagramas do documento
```

## Pré-requisitos

- Java 21
- Docker com Docker Compose
- Node.js (versão LTS suportada pelo Angular 22) e npm
- `curl` e `jq`, apenas para os testes por linha de comando

O Maven não precisa estar instalado: o backend usa o wrapper `./mvnw`.

## Como executar

### 1. Backend e banco de dados

```bash
cd backend
cp .env.example .env          # troque as senhas no arquivo criado
docker compose up -d          # sobe o PostgreSQL 16 na porta 5432
set -a && . ./.env && set +a  # carrega as variáveis do .env
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Na inicialização, o Flyway cria a tabela `usuarios`. O perfil `dev` cadastra dois usuários de exemplo, ambos com a senha definida em `SEED_SENHA_PADRAO`:

| Nome | E-mail |
|---|---|
| Maria Exemplo | `maria@exemplo.com` |
| João Exemplo | `joao@exemplo.com` |

A API fica disponível em `http://localhost:8080`, e a documentação Swagger, em `http://localhost:8080/swagger-ui.html` (desativada no perfil `prod`).

### 2. Frontend

Em outro terminal:

```bash
cd frontend
npm ci
npm start
```

Acesse `http://localhost:4200`. As rotas são `/login`, `/cadastro` e `/perfil` (esta, apenas com sessão ativa).

## API

| Endpoint | Finalidade | Acesso | Respostas |
|---|---|---|---|
| `GET /api/auth/csrf` | Cria ou reutiliza a sessão e emite o token CSRF | Público | 200 `{headerName, token}` |
| `POST /api/auth/login` | Autentica e cria a sessão | Público, com token CSRF | 200, 400, 401, 403, 429, 500 |
| `POST /api/auth/cadastro` | Cadastra um usuário (senha de 8 a 128 caracteres) | Público, com token CSRF | 201, 400, 403, 409 |
| `GET /api/auth/me` | Retorna o usuário da sessão | Sessão ativa | 200, 401 |
| `POST /api/auth/logout` | Invalida a sessão e remove o cookie | Token CSRF | 204, 403 |

Respostas do login:

| Status | Situação | Corpo |
|---|---|---|
| 200 | Credenciais válidas (cria a sessão) | `{"autenticado": true, "usuario": {"id": "...", "nome": "...", "email": "..."}}` |
| 400 | Campo vazio, e-mail mal formatado, JSON ilegível ou Content-Type incorreto | `{"erro": "ENTRADA_INVALIDA"}` |
| 401 | Usuário inexistente ou senha incorreta (resposta idêntica nos dois casos) | `{"autenticado": false, "erro": "CREDENCIAIS_INVALIDAS", "mensagem": "E-mail ou senha inválidos."}` |
| 403 | Token CSRF ausente ou inválido | `{"erro": "ACESSO_NEGADO", "mensagem": "Acesso negado."}` |
| 429 | Mais de 5 falhas por e-mail ou 20 por IP em 1 minuto (cabeçalho `Retry-After`) | `{"autenticado": false, "erro": "TENTATIVAS_EXCEDIDAS", ...}` |
| 500 | Erro inesperado | `{"erro": "ERRO_INTERNO"}` |

## Testar os cenários de autenticação

Com o backend em execução no perfil `dev` e as variáveis do `.env` carregadas no terminal. Cada requisição `POST` precisa do cookie da sessão e do token CSRF:

```bash
URL=http://localhost:8080/api/auth
novo_token() { curl -s -c cookies.txt -b cookies.txt "$URL/csrf" | jq -r .token; }
post() { curl -i -s -b cookies.txt -c cookies.txt -X POST "$URL/$1" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(novo_token)" -d "$2"; }

# 1. Credenciais válidas -> 200 e cookie de sessão
post login "{\"email\":\"maria@exemplo.com\",\"senha\":\"$SEED_SENHA_PADRAO\"}"

# 2. Usuário da sessão -> 200
curl -i -s -b cookies.txt "$URL/me"

# 3. Logout -> 204; em seguida, /me -> 401
post logout ''
curl -i -s -b cookies.txt "$URL/me"

# 4. Senha incorreta -> 401
post login '{"email":"maria@exemplo.com","senha":"senha-errada"}'

# 5. Usuário inexistente -> 401, com a mesma resposta do cenário 4
post login '{"email":"ninguem@exemplo.com","senha":"qualquer"}'

# 6. E-mail mal formatado -> 400
post login '{"email":"nao-e-email","senha":"qualquer"}'

# 7. Sem token CSRF -> 403
curl -i -s -b cookies.txt -X POST "$URL/login" -H 'Content-Type: application/json' \
  -d '{"email":"maria@exemplo.com","senha":"qualquer"}'

# 8. Cadastro -> 201; repetir o mesmo e-mail -> 409
post cadastro '{"nome":"Ana Teste","email":"ana@exemplo.com","senha":"Senha-Teste-123"}'
post cadastro '{"nome":"Ana Teste","email":"ana@exemplo.com","senha":"Senha-Teste-123"}'

# 9. Tentativas excedidas: a 6.ª falha seguida para o mesmo e-mail -> 429 com Retry-After
for i in 1 2 3 4 5 6; do post login '{"email":"joao@exemplo.com","senha":"errada"}' | head -1; done
```

Os testes automatizados cobrem esses cenários. No backend, eles sobem um PostgreSQL próprio via Testcontainers; no frontend, usam o Vitest:

```bash
(cd backend && ./mvnw -B verify)
(cd frontend && npm test)
```

## Encerrar

```bash
cd backend
docker compose down      # mantém os dados
docker compose down -v   # apaga também o volume do banco
```
