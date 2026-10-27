# software-autenticacao-web
Repositório destinado ao desenvolvimento de um Software Web para Autenticação de Usuários, como parte da Atividade Prática Supervisionada (APS) da disciplina Domínios de Software, semestre 2026/2, da Universidade Federal de Goiás (UFG).

## Como executar

Pré-requisitos: Java 21, Docker em execução e Node.js.

### 1. Backend (`backend/`)

```bash
cp .env.example .env          # ajuste as senhas se quiser
docker compose up -d          # sobe o PostgreSQL
set -a && . ./.env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # perfil dev cria usuários de teste
```

API em `http://localhost:8080` (Swagger em `/swagger-ui.html`). Detalhes, variáveis de ambiente e endpoints em [backend/README.md](backend/README.md).

### 2. Frontend (`frontend/`)

```bash
npm install
npm start
```

Acesse `http://localhost:4200`. As requisições a `/api` são redirecionadas para o backend via proxy. Detalhes em [frontend/README.md](frontend/README.md).
