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
cp .env.example .env   # preencha os valores
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

1. Crie o arquivo `.env` a partir do exemplo e preencha os valores (a aplicação o lê automaticamente da raiz do projeto):
   ```bash
   cp .env.example .env
   ```

2. Execute a aplicação:
   ```bash
   mvn spring-boot:run
   ```

## Testes

```bash
mvn test
```

Os testes de integração sobem um Oracle Database Free descartável via Testcontainers (imagem `gvenzl/oracle-free:23-slim-faststart`) e aplicam as migrations Flyway reais, então **o Docker precisa estar em execução**. Não é necessário `.env` nem um banco local: o banco e o segredo JWT dos testes são configurados automaticamente. A primeira execução baixa a imagem do Oracle; o container é criado uma única vez por execução e compartilhado por todos os testes.

| Camada | Classes | O que cobre |
|---|---|---|
| Unitários | `DimensionTest`, `JwtServiceTest`, `AppUserDetailsTest`, `JwtPropertiesTest` | Conversão de dimensão, emissão/validação de JWT, autoridades e validação de configuração |
| Repositórios | `SchemaMigrationTest`, `GoalRepositoryTest`, `TaskRepositoryTest`, `UserContributionRepositoryTest` | Migrations e seed, constraints, filtros por data e isolamento por empresa |
| API (ponta a ponta) | `AuthApiTest`, `SecurityApiTest`, `CompanyApiTest`, `GoalApiTest`, `TaskApiTest`, `AnalyticsApiTest`, `ErrorHandlingApiTest` | Rotas via MockMvc com filtro de segurança, JWT e banco reais |

## Variáveis de Ambiente

Definidas no arquivo `.env` (não versionado; modelo em `.env.example`). `DB_PASSWORD` e `JWT_SECRET` não têm valor padrão e precisam ser definidas. Sem `JWT_SECRET` (ou com menos de 32 caracteres) a aplicação aborta a subida com uma mensagem explícita; sem `DB_PASSWORD` ela falha ao conectar no banco (ORA-01017).

| Variável | Padrão | Descrição |
|---|---|---|
| DB_URL | jdbc:oracle:thin:@localhost:1521/FREEPDB1 | URL JDBC do Oracle |
| DB_USER | ecoal | Usuário do banco de dados |
| DB_PASSWORD | (obrigatória) | Senha do banco de dados |
| ORACLE_PASSWORD | (obrigatória no Compose) | Senha do SYS/SYSTEM do container Oracle |
| JWT_SECRET | (obrigatória) | Chave de assinatura JWT (mínimo 256 bits para HS256) |
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
