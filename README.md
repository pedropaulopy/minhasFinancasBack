# Minhas Financas

Este projeto implementa o **backend de um sistema de controle financeiro**, permitindo o gerenciamento de **usuários** e
**lançamentos financeiros** (receitas e despesas).  
Foi desenvolvido em **Java com Spring Boot**, seguindo boas práticas de arquitetura, DTOs, services e exceções
customizadas.

---

## Tecnologias Utilizadas

- **Java 17+**
- **Spring Boot 3.5.5** (Web, Data JPA, Validation)
- **PostgreSQL 18**
- **Maven 4.0.0** (modelo do POM)
- **JPA/Hibernate**
- **Lombok**
- **RESTful API**

> Observação: o `pom.xml` usa o *parent* `spring-boot-starter-parent:3.5.5` e o modelo de POM `4.0.0`.

---

## Funcionalidades

O backend oferece um conjunto de funcionalidades para o gerenciamento financeiro:

- **Gerenciamento de Lançamentos:**
    - **CRUD completo:** crie, edite, visualize e delete lançamentos (receitas e despesas).
    - **Busca flexível:** filtre lançamentos por descrição, mês, ano, valor, tipo e status.
    - **Atualização de status:** altere o status de um lançamento (ex: de PENDENTE para EFETIVADO).

- **Gerenciamento de Categorias:**
    - **CRUD completo:** crie, edite, visualize e delete categorias para organizar seus lançamentos.
    - **Busca por nome:** encontre categorias pelo nome.

- **Importação e Exportação de Dados:**
    - **Importação em lote via CSV:** envie um arquivo CSV com múltiplos lançamentos para importação em massa. O sistema processa os dados e retorna um relatório detalhado, indicando sucessos e falhas.
    - **Exportação em múltiplos formatos:**
        - **JSON:** exporte os dados dos lançamentos em formato JSON.
        - **CSV:** gere um arquivo CSV com os lançamentos, ideal para planilhas.
        - **Google Sheets:** exporte os dados diretamente para uma nova planilha no Google Drive do usuário.

---

## Estrutura do Projeto

```
development/
├── minhasFinancas/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │       ├── pedropaulo/
│   │   │           ├── minhas_financas/
│   │   │               ├── api/
│   │   │               │   ├── config/
│   │   │               │   │   ├── GoogleDriveConfig.java
│   │   │               │   │   └── SecurityConfig.java
│   │   │               │   ├── dto/
│   │   │               │   │   ├── exportacao/
│   │   │               │   │   │   ├── ExportLancamentosDTO.java
│   │   │               │   │   │   ├── ExportLancamentosSheetsDTO.java
│   │   │               │   │   │   ├── ExportSheetsErrosDTO.java
│   │   │               │   │   │   └── ExportSheetsResultadoDTO.java
│   │   │               │   │   ├── importacao/
│   │   │               │   │   │   ├── AuxiliarLinhaErro.java
│   │   │               │   │   │   └── ImportResultadoDTO.java
│   │   │               │   │   ├── CategoriaDTO.java
│   │   │               │   │   ├── LancamentoDTO.java
│   │   │               │   │   ├── LancamentoStatusDTO.java
│   │   │               │   │   ├── TokenDTO.java
│   │   │               │   │   └── UsuarioDTO.java
│   │   │               │   ├── handler/
│   │   │               │   │   └── GlobalExceptionHandler.java
│   │   │               │   ├── resource/
│   │   │               │   │   ├── CategoriaResource.java
│   │   │               │   │   ├── LancamentoResource.java
│   │   │               │   │   └── UsuarioResource.java
│   │   │               │   └── JwtTokenFilter.java
│   │   │               ├── exception/
│   │   │               │   ├── AutenticacaoException.java
│   │   │               │   ├── EntidadeNaoProcessavelException.java
│   │   │               │   └── RegraNegocioException.java
│   │   │               ├── model/
│   │   │               │   ├── entity/
│   │   │               │   │   ├── Categoria.java
│   │   │               │   │   ├── Lancamento.java
│   │   │               │   │   └── Usuario.java
│   │   │               │   ├── enums/
│   │   │               │   │   ├── StatusLancamento.java
│   │   │               │   │   └── TipoLancamento.java
│   │   │               │   ├── repository/
│   │   │               │       ├── CategoriaRepository.java
│   │   │               │       ├── LancamentoRepository.java
│   │   │               │       └── UsuarioRepository.java
│   │   │               ├── service/
│   │   │               │   ├── impl/
│   │   │               │   │   ├── CategoriaServiceImpl.java
│   │   │               │   │   ├── GoogleSheetsExportImpl.java
│   │   │               │   │   ├── JwtServiceImpl.java
│   │   │               │   │   ├── LancamentoCsvImportServiceImpl.java
│   │   │               │   │   ├── LancamentoExportServiceImpl.java
│   │   │               │   │   ├── LancamentoServiceImpl.java
│   │   │               │   │   ├── SecurityUserDetailsServiceImpl.java
│   │   │               │   │   └── UsuarioServiceImpl.java
│   │   │               │   ├── CategoriaService.java
│   │   │               │   ├── GoogleSheetsExport.java
│   │   │               │   ├── JwtService.java
│   │   │               │   ├── LancamentoCsvImportService.java
│   │   │               │   ├── LancamentoExportService.java
│   │   │               │   ├── LancamentoService.java
│   │   │               │   ├── RecordCreatedSheet.java
│   │   │               │   ├── SecurityUserDetailsService.java
│   │   │               │   └── UsuarioService.java
│   │   │               └── MinhasFinancasApplication.java
│   │   ├── resources/
│   │       ├── application.properties
│   │       └── client_secret.json
│   ├── test/
│       ├── java/
│           ├── com/
│           │   ├── pedropaulo/
│           │       ├── minhas_financas/
│           │           ├── api/
│           │           │   ├── dto/
│           │           │       ├── AuxiliarLinhaErroDTOTest.java
│           │           │       ├── ImportResultadoDTOTest.java
│           │           │       └── LancamentoDTOFactory.java
│           │           ├── exception/
│           │           │   └── GlobalExceptionHandlerTest.java
│           │           ├── model/
│           │           │   ├── repository/
│           │           │       ├── LancamentoRepositoryTest.java
│           │           │       └── UsuarioRepositoryTest.java
│           │           ├── resource/
│           │           │   ├── CategoriaResourceTest.java
│           │           │   ├── LancamentoResourceTest.java
│           │           │   └── UsuarioResourceTest.java
│           │           ├── service/
│           │           │   ├── impl/
│           │           │   │   ├── GoogleSheetsExportImplTest.java
│           │           │   │   ├── JwtServiceImplTest.java
│           │           │   │   ├── LancamentoCsvImportServiceImplTest.java
│           │           │   │   ├── LancamentoExportServiceImplTest.java
│           │           │   │   └── SecurityUserDetailsServiceImplTest.java
│           │           │   ├── testUtils/
│           │           │   │   ├── AuthMocks.java
│           │           │   │   └── StubUserDetailsService.java
│           │           │   ├── CategoriaServiceTest.java
│           │           │   ├── LancamentoServiceTest.java
│           │           │   └── UsuarioServiceTest.java
│           │           ├── JwtTokenFilterTest.java
│           │           └── MinhasFinancasApplicationTest.java
│           ├── resources/
│               ├── TestNoOpTransactionTemplate.java
│               └── application-test.properties
├── HELP.md
├── README.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

---

## Descrição das Camadas (Responsabilidades)

### **Model (Entidades do Domínio)**

Camada que **mapeia as regras do domínio para o banco de dados** usando JPA/Hibernate.  
Aqui ficam as classes de entidade (`Usuario`, `Lancamento`) e **enums** que expressam conceitos do domínio (
`TipoLancamento`, `StatusLancamento`).  
Responsabilidades principais:

- Definir **atributos persistentes** e **relacionamentos** (ex.: `@OneToMany`, `@ManyToOne`).
- Conter **anotações JPA** (`@Entity`, `@Table`, `@Column`) e **restrições**.
- Representar o **estado do negócio** sem lógica de apresentação.

### **DTO (Data Transfer Object)**

Objetos usados para **entrada e saída da API** (requests/responses), **desacoplando** o contrato REST das entidades do
domínio.  
Responsabilidades principais:

- **Validar** dados de entrada com `javax.validation` (ex.: `@NotNull`, `@Email`).
- **Evitar exposição** direta das entidades (especialmente campos sensíveis como senha).
- **Modelar payloads** específicos (ex.: alteração de status).

### **Service (Regras de Negócio)**

Camada que **orquestra os casos de uso** do sistema e **centraliza validações de negócio**.  
Responsabilidades principais:

- Implementar **regras e invariantes**, lançando **exceções de negócio** quando necessário (`RegraNegocioException`).
- **Transacionar** operações e coordenar chamadas aos repositórios.
- Garantir **consistência** e **integridade** do fluxo (ex.: mudança de status válida).

### **Repositories (Acesso a Dados)**

Interfaces JPA responsáveis pela **persistência** e **consulta** de dados.  
Responsabilidades principais:

- Estender `JpaRepository` e expor **métodos de CRUD** e **consultas derivadas**.
- Isolar o acesso ao **PostgreSQL** do restante da aplicação.
- Fornecer **consultas específicas** quando necessário.

### **Exceptions (Erros de Domínio e Autenticação)**

Tipos de exceções **semânticas** do sistema:

- `RegraNegocioException`: violações de regra de negócio.
- `AutenticacaoException`: falhas de autenticação/login.

### **Resources (Controllers REST)**

Adaptadores HTTP que **exponem endpoints REST** e **traduzem** requisições/respostas para DTOs.  
Responsabilidades principais:

- Mapear **rotas** e **métodos HTTP**.
- Realizar **binding/validação** de entrada.
- Delegar a execução para a **camada de serviço** e retornar respostas adequadas (HTTP 2xx/4xx/5xx).

---

## Endpoints

A API RESTful do Minhas Finanças oferece um conjunto completo de endpoints para gerenciar usuários, categorias e lançamentos financeiros.

### Autenticação

Todos os endpoints, exceto os de autenticação e registro de usuário, são protegidos e exigem um token JWT no cabeçalho `Authorization`.

**Formato do Cabeçalho:**
`Authorization: Bearer <seu-token-jwt>`

---

### Usuário (`/api/usuarios`)

#### `POST /api/usuarios/autenticar`

Autentica um usuário e retorna um token JWT.

**Request Body:**
```json
{
  "email": "usuario@exemplo.com",
  "senha": "sua-senha"
}
```

**Response (200 OK):**
```json
{
  "nome": "Nome do Usuário",
  "email": "usuario@exemplo.com",
  "token": "seu-token-jwt"
}
```

---

#### `POST /api/usuarios`

Registra um novo usuário no sistema.

**Request Body:**
```json
{
  "nome": "Novo Usuário",
  "email": "novo@exemplo.com",
  "senha": "sua-senha-forte"
}
```

**Response (201-Created):**
```json
{
    "id": 1,
    "nome": "Novo Usuário",
    "email": "novo@exemplo.com",
    "dataCadastro": "2025-11-10T14:30:00"
}
```

---

#### `GET /api/usuarios/{id}/saldo`

Obtém o saldo total (receitas - despesas) de um usuário.

**Path Variable:**
- `{id}`: ID do usuário.

**Response (200 OK):**
```json
1500.75
```

---

### Categorias (`/api/categorias`)

#### `POST /api/categorias`

Cria uma nova categoria para o usuário autenticado.

**Request Body:**
```json
{
  "nome": "Alimentação"
}
```

**Response (201-Created):**
```json
{
    "id": 1,
    "nome": "Alimentação",
    "usuario": {
        "id": 10,
        "nome": "Nome do Usuário"
    }
}
```

---

#### `GET /api/categorias`

Busca categorias do usuário autenticado.

**Query Params (Opcionais):**
- `nome`: Filtra categorias pelo nome.

**Response (200 OK):**
```json
[
    {
        "id": 1,
        "nome": "Alimentação"
    },
    {
        "id": 2,
        "nome": "Transporte"
    }
]
```

---

#### `PUT /api/categorias/{id}`

Atualiza o nome de uma categoria.

**Path Variable:**
- `{id}`: ID da categoria.

**Request Body:**
```json
{
  "nome": "Alimentação e Mercado"
}
```

**Response (200 OK):**
```json
{
    "id": 1,
    "nome": "Alimentação e Mercado"
}
```

---

#### `DELETE /api/categorias/{id}`

Deleta uma categoria.

**Path Variable:**
- `{id}`: ID da categoria.

**Response (204 No Content)**

---

### Lançamentos (`/api/lancamentos`)

#### `POST /api/lancamentos`

Cria um novo lançamento (receita ou despesa).

**Request Body:**
```json
{
    "descricao": "Salário Mensal",
    "mes": 11,
    "ano": 2025,
    "valor": 5000.00,
    "tipoLancamento": "RECEITA",
    "statusLancamento": "EFETIVADO",
    "categoriaId": [1]
}
```

**Response (201-Created):**
```json
{
    "id": 101,
    "descricao": "Salário Mensal",
    "mes": 11,
    "ano": 2025,
    "valor": 5000.00,
    "tipoLancamento": "RECEITA",
    "statusLancamento": "EFETIVADO",
    "usuario": { "id": 1, "nome": "Nome do Usuário" },
    "categorias": [{ "id": 1, "nome": "Salário" }]
}
```

---

#### `GET /api/lancamentos`

Busca lançamentos com base em filtros.

**Query Params (Opcionais):**
- `descricao`: Filtra por descrição.
- `mes`: Filtra por mês (1-12).
- `ano`: Filtra por ano.
- `valor`: Filtra por valor exato.
- `tipo_lancamento`: `RECEITA` ou `DESPESA`.
- `status_lancamento`: `PENDENTE`, `EFETIVADO` ou `CANCELADO`.
- `categoriaId`: Lista de IDs de categorias.

**Response (200 OK):**
```json
[
    {
        "id": 101,
        "descricao": "Salário Mensal",
        "valor": 5000.00,
        "tipoLancamento": "RECEITA",
        "statusLancamento": "EFETIVADO"
    }
]
```

---

#### `PUT /api/lancamentos/{id}`

Atualiza um lançamento existente.

**Path Variable:**
- `{id}`: ID do lançamento.

**Request Body:**
```json
{
    "descricao": "Salário Mensal Atualizado",
    "valor": 5100.00
}
```

**Response (200 OK):**
```json
{
    "id": 101,
    "descricao": "Salário Mensal Atualizado",
    "valor": 5100.00,
    "statusLancamento": "PENDENTE"
}
```

---

#### `PUT /api/lancamentos/{id}/atualizar-status`

Atualiza o status de um lançamento.

**Path Variable:**
- `{id}`: ID do lançamento.

**Request Body:**
```json
{
  "status": "EFETIVADO"
}
```

**Response (204 No Content)**

---

#### `DELETE /api/lancamentos/{id}`

Deleta um lançamento.

**Path Variable:**
- `{id}`: ID do lançamento.

**Response (204 No Content)**

---

#### `POST /api/lancamentos/upload`

Importa lançamentos em lote a partir de um arquivo CSV.

**Request (multipart/form-data):**
- `file`: Arquivo CSV com os lançamentos.

**Response (200 OK ou 207 Multi-Status):**
```json
{
    "totalLidas": 10,
    "totalSucesso": 8,
    "totalFalha": 2,
    "erros": [
        {
            "linha": 5,
            "motivo": "Valor inválido",
            "raw": "Compra,abc,RECEITA,..."
        }
    ]
}
```

---

#### `GET /api/lancamentos/export`

Exporta lançamentos em formato CSV ou JSON.

**Query Params:**
- `tipoExport`: `csv` (padrão) ou `json`.
- Outros filtros de busca de lançamento (opcionais).

**Response (200 OK):**
- O endpoint retorna um arquivo para download (`lancamentos.csv` ou `lancamentos.json`).

---

#### `GET /api/lancamentos/export?formato=sheets`

Exporta lançamentos diretamente para uma nova planilha no Google Sheets.

**Query Params:**
- `nomePlanilha`: Nome da planilha a ser criada (opcional).
- `folderId`: ID da pasta no Google Drive onde a planilha será criada (opcional).
- Outros filtros de busca de lançamento (opcionais).

**Response (200 OK):**
```json
{
    "id": "id-da-planilha-no-google-drive",
    "webViewLink": "link-para-visualizar-a-planilha",
    "webContentLink": "link-para-download-direto"
}
```

## Banco de Dados

O projeto utiliza **PostgreSQL** como banco de dados relacional.  
A conexão é configurada no arquivo `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/minhasFinancas
spring.datasource.username=postgres
spring.datasource.password=090833
spring.datasource.driver-class-name=org.postgresql.Driver
# Porta do servidor
server.port=8081
```

### Explicação dos Campos

| Propriedade                           | Descrição                                                                         |
|:--------------------------------------|:----------------------------------------------------------------------------------|
| `spring.datasource.url`               | URL de conexão com o banco PostgreSQL. Inclui o nome do banco (`minhasFinancas`). |
| `spring.datasource.username`          | Usuário do banco de dados.                                                        |
| `spring.datasource.password`          | Senha do banco de dados.                                                          |
| `spring.datasource.driver-class-name` | Driver JDBC utilizado para comunicação com o PostgreSQL.                          |
| `server.port`                         | Porta em que a aplicação Spring Boot será executada.                              |

**Criação do banco (exemplo):**

```sql
CREATE
DATABASE "minhasFinancas";
```

## Build e Execução

### Requisitos

- Java 17+ instalado
- PostgreSQL 18 em execução
- Maven instalado (ou usar o wrapper)

### Executar a aplicação

```bash
mvn spring-boot:run
# ou
mvn clean package && java -jar target/minhasFinancas-0.0.1-SNAPSHOT.jar
```

A API ficará disponível em:  
`http://localhost:8081/api`

---

## Segurança (JWT) e alterações nos testes

Com a introdução da camada de segurança baseada em JWT, alguns testes foram criados e outros ajustados para contemplar o
novo comportamento. Abaixo um resumo das mudanças e orientações para execução.

Principais testes novos/alterados:

- JwtServiceImplTest
    - Testa geração, validação e extração de claims de tokens JWT.
    - Usa ReflectionTestUtils para configurar `expiracao` e `chaveAssinatura` locais; não exige variáveis de ambiente
      para o teste unitário.
- JwtTokenFilterTest
    - Testa o filtro que intercepta requests e autentica pelo header `Authorization: Bearer <token>`.
    - Verifica fluxo com token válido, ausência do header, header mal formado e token inválido.
- SecurityUserDetailsServiceImplTest
    - Testa a carga de usuário pelo email e o mapeamento para UserDetails (roles, username, password).
- UsuarioResourceTest (ajustado)
    - Continua testando endpoints de autenticação/registro/saldo.
    - Agora mocka o serviço de JWT (por exemplo, `JwtServiceImpl`) quando necessário e/ou exclui a configuração de
      segurança para testes MVC com `@WebMvcTest(..., excludeAutoConfiguration = { SecurityAutoConfiguration.class })`.
    - Ex.: ao autenticar, o teste espera receber um token falso gerado pelo `jwtService.gerarToken(usuario)`.
- LancamentoResourceTest (ajustado)
    - Testes de controller continuam funcionais; em ambientes com segurança ativada, é necessário simular autenticação (
      mockar serviços ou configurar header Authorization) ou desativar segurança no teste com
      `excludeAutoConfiguration`.
