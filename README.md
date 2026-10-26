# software-autenticacao-web

Software Web para Autenticação de Usuários, desenvolvido como Atividade Prática Supervisionada (APS) da disciplina **Domínios de Software**, semestre 2026/2, do Bacharelado em Engenharia de Software da Universidade Federal de Goiás (UFG). Docente: Profa. Adriana Silveira de Souza.

**Equipe:** Claudio Cristiano Louza Filho, Claudio Eliziario Silva Ferreira e Pedro Augusto Rodrigues.

## O que a aplicação faz

A aplicação recebe e-mail e senha, verifica se correspondem a um usuário cadastrado e, em caso de sucesso, devolve a identidade desse usuário (identificador, nome e e-mail). Em caso de falha, devolve um erro de autenticação, sem indicar se o e-mail existe ou se a senha está errada.

Ficam fora do escopo: cadastro de usuários, recuperação ou alteração de senha, emissão de tokens ou controle de sessão, autorização e perfis de acesso.

## Arquitetura

| Camada | Tecnologia | Porta |
|---|---|---|
| Apresentação | Angular 22 (SPA executada no navegador) | — |
| Servidor Web (desenvolvimento) | Angular CLI (`ng serve`) | 4200 |
| Servidor de aplicação | Spring Boot 4.1 com Tomcat embutido, Java 21 | 8080 |
| Repositório de dados | PostgreSQL 16 em contêiner Docker | 5432 |

O navegador obtém a interface na porta 4200 e chama a API diretamente na porta 8080. O backend libera, via CORS, apenas a origem `http://localhost:4200`. As senhas são armazenadas somente como hash Argon2id.

As fontes dos diagramas do projeto (componentes, implantação, classes, atividades e sequência) e os comandos para gerar as imagens estão em [`docs/diagramas`](docs/diagramas).

## Estrutura do repositório

```
backend/        API Spring Boot (detalhes em backend/README.md)
frontend/       Interface Angular
docs/diagramas/ Fontes (.dot e .mmd) dos diagramas do documento
```

## Pré-requisitos

- Java 21
- Docker com Docker Compose
- Node.js (versão LTS suportada pelo Angular 22) e npm

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

A API fica disponível em `http://localhost:8080`.

### 2. Frontend

Em outro terminal:

```bash
cd frontend
npm ci
npm start
```

Acesse `http://localhost:4200/login`.

## API

`POST /api/auth/login`, com `Content-Type: application/json` e corpo `{ "email": "...", "senha": "..." }`.

| Status | Situação | Corpo |
|---|---|---|
| 200 | Credenciais válidas | `{"autenticado": true, "usuario": {"id": "...", "nome": "...", "email": "..."}}` |
| 400 | Campo vazio, e-mail mal formatado, JSON ilegível ou Content-Type incorreto | `{"autenticado": false, "erro": "ENTRADA_INVALIDA"}` |
| 401 | Usuário inexistente ou senha incorreta (resposta idêntica nos dois casos) | `{"autenticado": false, "erro": "CREDENCIAIS_INVALIDAS", "mensagem": "E-mail ou senha inválidos."}` |
| 500 | Erro inesperado | `{"autenticado": false, "erro": "ERRO_INTERNO"}` |

## Testar os cenários de autenticação

Com o backend em execução e as variáveis do `.env` carregadas no terminal:

```bash
URL=http://localhost:8080/api/auth/login

# 1. Credenciais válidas -> 200
curl -i -X POST $URL -H 'Content-Type: application/json' \
  -d "{\"email\":\"maria@exemplo.com\",\"senha\":\"$SEED_SENHA_PADRAO\"}"

# 2. Senha incorreta -> 401
curl -i -X POST $URL -H 'Content-Type: application/json' \
  -d '{"email":"maria@exemplo.com","senha":"senha-errada"}'

# 3. Usuário inexistente -> 401, com a mesma resposta do cenário 2
curl -i -X POST $URL -H 'Content-Type: application/json' \
  -d '{"email":"ninguem@exemplo.com","senha":"qualquer"}'

# 4. E-mail mal formatado -> 400
curl -i -X POST $URL -H 'Content-Type: application/json' \
  -d '{"email":"nao-e-email","senha":"qualquer"}'

# 5. Campo ausente -> 400
curl -i -X POST $URL -H 'Content-Type: application/json' \
  -d '{"email":"maria@exemplo.com"}'
```

Os testes automatizados do backend cobrem os mesmos cenários e sobem um PostgreSQL próprio via Testcontainers:

```bash
cd backend
./mvnw -B verify
```

## Encerrar

```bash
cd backend
docker compose down      # mantém os dados
docker compose down -v   # apaga também o volume do banco
```
