# Game Library Manager

API REST para gerenciar sua biblioteca pessoal de jogos com integração direta à Steam. Em vez de cadastrar jogo por jogo, você conecta sua conta da Steam e importa toda a sua biblioteca automaticamente — jogos jogados, tempo de jogo incluído.

## Funcionalidades

- Cadastro, edição e remoção de jogos manualmente
- Importação automática da biblioteca da Steam via API
- Organização por categorias
- Controle de status de cada jogo (jogando, finalizado, abandonado, planejado)
- Sistema de avaliação (nota de 1 a 10)
- Rastreamento de conquistas/platina por jogo
- Busca por nome, gênero, plataforma, status e ano de lançamento

## Tecnologias

- **Java 25** + **Spring Boot 4**
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL**
- **Spring Validation**
- **RestTemplate** (consumo da API da Steam)

## Pré-requisitos

- Java 25+
- Maven
- PostgreSQL rodando em `localhost:5432`

## Configuração do banco

Crie um banco de dados PostgreSQL com as seguintes credenciais (ou ajuste em `application.properties`):

```
database: gamelibrary
username: admin
password: admin
```

O Hibernate cria as tabelas automaticamente (`ddl-auto=update`).

## Como rodar

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

---

## Endpoints

### Jogos — `/jogos`

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/jogos` | Lista todos os jogos |
| POST | `/jogos` | Cadastra um novo jogo |
| GET | `/jogos/{id}` | Busca jogo por ID |
| PUT | `/jogos/{id}` | Atualiza jogo por ID |
| DELETE | `/jogos/{id}` | Remove jogo por ID |
| DELETE | `/jogos/deleteAllJogos` | Remove todos os jogos |
| PUT | `/jogos/{id}/platina` | Ativa rastreamento de conquistas/platina |
| GET | `/jogos/buscar/nome?nome=` | Busca por nome (parcial, case-insensitive) |
| GET | `/jogos/buscar/genero?genero=` | Busca por gênero |
| GET | `/jogos/buscar/plataforma?plataforma=` | Busca por plataforma |
| GET | `/jogos/buscar/status?status=` | Busca por status |
| GET | `/jogos/buscar/anoLancamento?anoLancamento=` | Busca por ano de lançamento |

**Body para criar/atualizar jogo:**
```json
{
  "nomeJogo": "Elden Ring",
  "generoJogo": "RPG",
  "plataformaJogo": "PC",
  "anoLancamentoJogo": 2022,
  "horasJogadas": 120.5,
  "statusJogo": "FINALIZADO",
  "notaJogo": 10,
  "desenvolvedoraJogo": "FromSoftware"
}
```

**Status disponíveis:** `JOGANDO` | `FINALIZADO` | `ABANDONADO` | `PLANEJADO`

---

### Categorias — `/categorias`

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/categorias` | Lista todas as categorias |
| POST | `/categorias` | Cria uma categoria |
| POST | `/categorias/manyCategories` | Cria múltiplas categorias |
| GET | `/categorias/{id}` | Busca categoria por ID |
| GET | `/categorias/nome?nome=` | Busca categoria por nome |
| DELETE | `/categorias/{id}` | Remove categoria por ID |
| DELETE | `/categorias/deleteAll` | Remove todas as categorias |

---

### Integração Steam — `/importSteam`

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/importSteam/importar?steamId=&apiKey=` | Importa todos os jogos da biblioteca Steam |

**Como usar:**

1. Obtenha sua **Steam API Key** em [steamcommunity.com/dev/apikey](https://steamcommunity.com/dev/apikey)
2. Descubra seu **Steam ID** em [steamidfinder.com](https://steamidfinder.com) ou no seu perfil da Steam
3. Faça a requisição:

```http
POST /importSteam/importar?steamId=SEU_STEAM_ID&apiKey=SUA_API_KEY
```

O sistema consulta a API da Steam, converte o tempo de jogo de minutos para horas e salva todos os jogos com status `PLANEJADO`. Após a importação, você pode editar cada jogo individualmente para adicionar nota, gênero, etc.

---

## Modelo de dados

### Jogo

| Campo | Tipo | Descrição |
|-------|------|-----------|
| idJogo | Long | ID gerado automaticamente |
| nomeJogo | String | Nome do jogo |
| generoJogo | String | Gênero |
| plataformaJogo | String | Plataforma |
| anoLancamentoJogo | int | Ano de lançamento (1958–2026) |
| horasJogadas | double | Horas jogadas |
| statusJogo | Enum | Status atual |
| notaJogo | double | Avaliação (1–10) |
| desenvolvedoraJogo | String | Desenvolvedora |
| platina | Platina | Dados de conquistas (opcional) |
| categoria | Categoria | Categoria do jogo |

### Platina / Conquistas

```json
{
  "platinaAtiva": true,
  "conquistaObtida": 45,
  "totalConquista": 60
}
```

---

## Estrutura do projeto

```
src/main/java/com/gamelibrary/gamelibrarymanager/
├── controller/
│   ├── GameController.java
│   ├── CategoryController.java
│   └── SteamController.java
├── service/
│   ├── GameService.java
│   ├── CategoriaService.java
│   └── SteamService.java
├── repository/
│   ├── JogoRepository.java
│   └── CategoriaRepository.java
├── model/
│   ├── Jogo.java
│   ├── Categoria.java
│   ├── Platina.java
│   └── GameStatus.java (enum)
├── dto/
│   ├── JogoRequestDTO.java
│   ├── JogoResponseDTO.java
│   ├── CategoryRequestDTO.java
│   ├── CategoryResponseDTO.java
│   └── steam/
│       ├── SteamGameDTO.java
│       └── SteamResponseDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── JogoNaoEncontradoException.java
│   ├── JogoNaoDeletadoException.java
│   └── CategoriaNaoEncontrada.java
└── AppConfig.java
```
