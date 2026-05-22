# ESG EcoAl API

API REST para o aplicativo Android EcoAL, permitindo que empresas acompanhem metas ESG, tarefas e métricas de desempenho.

## Tecnologias

| Dependência | Versão | Finalidade |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.3.4 | Framework base da aplicação |
| Spring Boot Web | — | Exposição de endpoints REST |
| Spring Boot Security | — | Autenticação e autorização |
| Spring Boot Data JPA | — | Mapeamento objeto-relacional (ORM) |
| Spring Boot Validation | — | Validação de entrada de dados (`@NotBlank`, `@Email`, etc.) |
| Flyway | — | Migrations de banco de dados |
| Oracle JDBC (ojdbc11) | 23.5.0.24.07 | Driver de conexão com Oracle Database 23ai |
| JJWT | 0.12.6 | Geração e validação de tokens JWT |
| Lombok | — | Redução de boilerplate com anotações (`@Data`, `@Getter`, etc.) |
| SpringDoc OpenAPI | 2.6.0 | Documentação automática da API via Swagger UI |
| Docker / Docker Compose | — | Containerização e ambiente local |

## Inicialização Rápida (Docker — recomendado)

```bash
docker compose up --build
```

API disponível em http://localhost:8080  
Swagger UI em http://localhost:8080/swagger-ui.html

**Credenciais do seed padrão:**

| E-mail | Senha | Perfil |
|---|---|---|
| alice@example.com | password | admin |
| bob@example.com | password | user |

## Desenvolvimento Local

**Pré-requisitos:** Java 21, Maven 3.9+, Oracle Database 23ai Free

A forma mais simples de rodar o Oracle localmente é via Docker:

```bash
docker run -d --name oracle-local \
  -e ORACLE_PASSWORD=ecoal \
  -e APP_USER=ecoal \
  -e APP_USER_PASSWORD=ecoal \
  -p 1521:1521 \
  gvenzl/oracle-free:23-slim
```

1. Configure as variáveis de ambiente (ou use os valores padrão):
   ```bash
   export DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
   export DB_USER=ecoal
   export DB_PASSWORD=ecoal
   export JWT_SECRET=your-secret
   ```

2. Execute a aplicação:
   ```bash
   mvn spring-boot:run
   ```

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| DB_URL | jdbc:oracle:thin:@localhost:1521/FREEPDB1 | URL JDBC do Oracle |
| DB_USER | ecoal | Usuário do banco de dados |
| DB_PASSWORD | ecoal | Senha do banco de dados |
| JWT_SECRET | ecoal-dev-secret-key-... | Chave de assinatura JWT (mínimo 256 bits para HS256) |
| SPRING_PROFILES_ACTIVE | (nenhum) | Definir como `docker` ao rodar em container |

## Rotas

### Autenticação

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| POST | /api/auth/sign-up | Público | Cadastra usuário (empresa deve existir via seed) |
| POST | /api/auth/sign-in | Público | Login, retorna JWT |
| GET | /api/auth/me | Bearer | Retorna o usuário autenticado |

### Empresas

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| POST | /api/companies | Bearer | Cria empresa |
| GET | /api/companies/:id | Bearer | Busca empresa (escopo por tenant) |
| PUT | /api/companies/:id | Bearer | Atualiza empresa |
| DELETE | /api/companies/:id | Bearer | Remove empresa |

### Metas

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| GET | /api/goals | Bearer | Lista metas (filtros: dimension, created_at) |
| POST | /api/goals | Bearer | Cria meta (máx. 10 por dimensão) |
| GET | /api/goals/:id | Bearer | Busca meta com tarefas |
| PUT | /api/goals/:id | Bearer | Atualiza meta |
| DELETE | /api/goals/:id | Bearer | Remove meta |

### Tarefas

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| GET | /api/goals/:goalId/tasks | Bearer | Lista tarefas (filtros: completed, created_at) |
| POST | /api/goals/:goalId/tasks | Bearer | Cria tarefa (máx. 10 por meta) |
| GET | /api/goals/:goalId/tasks/:id | Bearer | Busca tarefa |
| PUT | /api/goals/:goalId/tasks/:id | Bearer | Atualiza tarefa |
| DELETE | /api/goals/:goalId/tasks/:id | Bearer | Remove tarefa |

### Analíticos

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| GET | /api/analytics/dimensions | Bearer | Progresso por dimensão ESG (period: monthly\|quarterly\|annual) |
| GET | /api/analytics/score | Bearer | Score da empresa |
| GET | /api/analytics/dashboard | Bearer | Dashboard (departamentos, contribuições, energia, treinamento) |

## Schemas das Entidades

### Company

| Campo | Tipo | Descrição |
|---|---|---|
| id | Long | Gerado automaticamente |
| name | String | Nome da empresa |
| cnpj | String | Identificador único |
| global_score_goal | Double | Meta de score global (padrão: 100) |

### User

| Campo | Tipo | Descrição |
|---|---|---|
| id | Long | Gerado automaticamente |
| name | String | Nome completo |
| email | String | Único |
| role | String | Perfil do usuário (admin, user, etc.) |
| company_id | Long | FK para companies |
| department | String | Nome do departamento |

### Goal

| Campo | Tipo | Descrição |
|---|---|---|
| id | Long | Gerado automaticamente |
| title | String | Título da meta |
| description | String | Opcional |
| dimension | String | environmental, social, governance |
| company_id | Long | FK para companies |

### Task

| Campo | Tipo | Descrição |
|---|---|---|
| id | Long | Gerado automaticamente |
| title | String | Título da tarefa |
| description | String | Opcional |
| score | Double | Valor de pontuação (padrão: 0) |
| completed | Boolean | Status de conclusão |
| completed_at | Timestamp | Definido automaticamente ao concluir |
| goal_id | Long | FK para goals |

## Regras de Negócio

- Máximo de 10 metas por dimensão ESG por empresa
- Máximo de 10 tarefas por meta
- Todos os dados são isolados por empresa (multi-tenant)
- O campo `completed_at` é definido automaticamente quando `completed` passa para `true`
