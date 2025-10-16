# Minhas Financas

Este projeto implementa o **backend de um sistema de controle financeiro**, permitindo o gerenciamento de **usuários** e **lançamentos financeiros** (receitas e despesas).  
Foi desenvolvido em **Java com Spring Boot**, seguindo boas práticas de arquitetura, DTOs, services e exceções customizadas.

---

##  Tecnologias Utilizadas

- **Java 17+**
- **Spring Boot 3.5.5** (Web, Data JPA, Validation)
- **PostgreSQL 18**
- **Maven 4.0.0** (modelo do POM)
- **JPA/Hibernate**
- **Lombok**
- **RESTful API**

> Observação: o `pom.xml` usa o *parent* `spring-boot-starter-parent:3.5.5` e o modelo de POM `4.0.0`.

---

##  Estrutura do Projeto

```
src/
 └── main/
     ├── java/
     │   └── .com.pedropaulo.minhasFinancas/
     │       ├── model/
     │       │   ├── Lancamento.java
     │       │   ├── Usuario.java
     │       │   ├── TipoLancamento.java
     │       │   └── StatusLancamento.java
     │       │
     │       ├── dto/
     │       │   ├── LancamentoDTO.java
     │       │   ├── LancamentoStatusDTO.java
     │       │   └── UsuarioDTO.java
     │       │
     │       ├── exception/
     │       │   ├── RegraNegocioException.java
     │       │   └── AutenticacaoException.java
     │       │
     │       ├── repository/
     │       │   ├── LancamentoRepository.java
     │       │   └── UsuarioRepository.java
     │       │
     │       ├── service/
     │       │   ├── LancamentoService.java
     │       │   ├── LancamentoServiceImpl.java
     │       │   ├── UsuarioService.java
     │       │   └── UsuarioServiceImpl.java
     │       │
     │       └── resource/
     │           ├── LancamentoResource.java
     │           └── UsuarioResource.java
     │
     └── resources/
         └── application.properties
```

---

##  Descrição das Camadas (Responsabilidades)

### **Model (Entidades do Domínio)**
Camada que **mapeia as regras do domínio para o banco de dados** usando JPA/Hibernate.  
Aqui ficam as classes de entidade (`Usuario`, `Lancamento`) e **enums** que expressam conceitos do domínio (`TipoLancamento`, `StatusLancamento`).  
Responsabilidades principais:
- Definir **atributos persistentes** e **relacionamentos** (ex.: `@OneToMany`, `@ManyToOne`).
- Conter **anotações JPA** (`@Entity`, `@Table`, `@Column`) e **restrições**.
- Representar o **estado do negócio** sem lógica de apresentação.

### **DTO (Data Transfer Object)**
Objetos usados para **entrada e saída da API** (requests/responses), **desacoplando** o contrato REST das entidades do domínio.  
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

##  Banco de Dados

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

###  Explicação dos Campos
| Propriedade | Descrição |
|:--|:--|
| `spring.datasource.url` | URL de conexão com o banco PostgreSQL. Inclui o nome do banco (`minhasFinancas`). |
| `spring.datasource.username` | Usuário do banco de dados. |
| `spring.datasource.password` | Senha do banco de dados. |
| `spring.datasource.driver-class-name` | Driver JDBC utilizado para comunicação com o PostgreSQL. |
| `server.port` | Porta em que a aplicação Spring Boot será executada. |

**Criação do banco (exemplo):**
```sql
CREATE DATABASE "minhasFinancas";
```

##  Build e Execução

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



