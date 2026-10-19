# Game Library Manager

API REST para gerenciar sua biblioteca pessoal de jogos com integração direta à Steam. Em vez de cadastrar jogo por jogo, você conecta sua conta da Steam e importa toda a sua biblioteca automaticamente — jogos jogados, tempo de jogo incluído.

## Funcionalidades

- Cadastro, edição e remoção de jogos manualmente
- Importação automática da biblioteca da Steam via API, sem duplicar jogos já importados
- Status definido automaticamente na importação, a partir das horas jogadas
- Organização por categorias
- Controle de status de cada jogo (jogando, finalizado, abandonado, planejado)
- Sistema de avaliação (nota de 1 a 10)
- Meta de platina por jogo (opcional), com o progresso de conquistas buscado na Steam
- Checklist de conquistas: o que já foi feito e o que falta
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

## Configuração da Steam

A chave da API e o seu Steam ID ficam num arquivo `.env` na raiz do projeto. Esse arquivo está no `.gitignore`, então a sua chave nunca vai para o GitHub.

1. Copie o modelo: `cp .env.example .env`
2. Preencha os valores, sem aspas e sem espaços:

```
STEAM_API_KEY=sua_chave
STEAM_ID=76561198xxxxxxxxx
```

- **API Key:** [steamcommunity.com/dev/apikey](https://steamcommunity.com/dev/apikey)
- **Steam ID:** use o SteamID64, com 17 dígitos e começando com `7656`. Ele aparece em [steamid.io](https://steamid.io).

## Como rodar

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Testes

```bash
./mvnw test -Dtest=SteamServiceStatusTest
```

São testes unitários da regra de status da importação. Eles rodam sem banco e sem acessar a Steam.

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
| PUT | `/jogos/{id}/platina` | Define a meta de platina; em jogos da Steam, busca o progresso automaticamente |
| DELETE | `/jogos/{id}/platina` | Remove a platina e o checklist (o jogo não é apagado) |
| GET | `/jogos/{id}/conquistas?concluida=` | Checklist de conquistas (`concluida=false` mostra só as que faltam) |
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
| POST | `/importSteam/importar` | Importa a biblioteca da Steam do `STEAM_ID` configurado no `.env` |
| POST | `/importSteam/importar?steamId=` | Importa a biblioteca de outro perfil |

**Como usar:** configure o `.env` (veja [Configuração da Steam](#configuração-da-steam)) e faça a requisição:

```http
POST http://localhost:8080/importSteam/importar
```

**O que a importação faz:**

- Converte o tempo de jogo de minutos para horas.
- **Não duplica:** cada jogo guarda o seu `steamAppId`, e os jogos que já foram importados são ignorados. A resposta traz só os jogos novos (`[]` se não houver nada novo).
- **Define o status inicial** de cada jogo:

| Situação na Steam | Status |
|---|---|
| Nunca jogou | `PLANEJADO` |
| Jogou nas últimas 2 semanas | `JOGANDO` |
| Não joga há 2 semanas e tem 10h ou mais | `FINALIZADO` |
| Não joga há 2 semanas e tem menos de 10h | `ABANDONADO` |

> O status é uma estimativa baseada nas horas jogadas. Jogos online, ou zerados em poucas horas, podem precisar de ajuste manual com `PUT /jogos/{id}`.

**Erros (422, com mensagem explicando o que fazer):** chave não configurada ou recusada pela Steam, Steam ID fora do formato de 17 dígitos, ou perfil com os detalhes de jogo privados.

Depois de importar, você pode editar cada jogo para adicionar nota, gênero etc.

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
| steamAppId | Long | ID do jogo na Steam (único; vazio em jogos cadastrados manualmente) |
| platina | Platina | Meta de platina e conquistas (opcional; não aparece se não foi definida) |
| categoria | Categoria | Categoria do jogo |

### Platina / Conquistas

A platina é **opcional**. Ela marca se você tem a meta de platinar o jogo (`platinaAtiva`) e quanto já avançou.

**Jogos importados da Steam:** basta ativar. A API busca as conquistas na Steam, preenche os números e monta o checklist.

```http
PUT http://localhost:8080/jogos/{id}/platina
{ "platinaAtiva": true }
```

Para atualizar o progresso depois de desbloquear mais conquistas, é só chamar de novo.

**Jogos de outras plataformas** (cadastrados à mão): informe os números.

```json
{ "platinaAtiva": true, "conquistaObtida": 45, "totalConquista": 60 }
```

- `platinaAtiva` é obrigatório: `true` ativa a meta, e `false` desiste dela sem apagar o progresso.
- `conquistaObtida` e `totalConquista` são opcionais, mas vão juntos. As obtidas não podem passar do total.
- Se você mandar os números, eles valem mais que os da Steam.

**Na resposta**, a platina ganha dois campos calculados: `conquistaRestante` (quantas faltam) e `platinado` (`true` quando obtidas = total).

**Erros (422):** o jogo não tem conquistas na Steam, ou os detalhes de jogo do seu perfil estão privados.

### Checklist de conquistas

```http
GET http://localhost:8080/jogos/{id}/conquistas?concluida=false
```

```json
[
  { "nome": "Lorde Prístino", "descricao": "", "concluida": false },
  { "nome": "Portador de Fragmento, Godrick", "descricao": "", "concluida": true, "dataDesbloqueio": "2025-01-05T04:13:38Z" }
]
```

- Sem o parâmetro `concluida`, a lista traz todas as conquistas; com `true`, só as feitas; com `false`, só as que faltam.
- Nomes e descrições vêm em português quando o jogo tem tradução.
- Conquistas secretas vêm com a descrição vazia, porque a Steam esconde esse texto.

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
│   ├── CategoriaRepository.java
│   └── ConquistaRepository.java
├── model/
│   ├── Jogo.java
│   ├── Categoria.java
│   ├── Platina.java
│   ├── Conquista.java
│   └── GameStatus.java (enum)
├── dto/
│   ├── JogoRequestDTO.java
│   ├── JogoResponseDTO.java
│   ├── CategoryRequestDTO.java
│   ├── CategoryResponseDTO.java
│   ├── PlatinaRequestDTO.java
│   ├── ConquistaResponseDTO.java
│   └── steam/
│       ├── SteamGameDTO.java
│       ├── SteamResponseDTO.java
│       ├── SteamConquistaDTO.java
│       └── SteamConquistasResponseDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── JogoNaoEncontradoException.java
│   ├── JogoNaoDeletadoException.java
│   ├── CategoriaNaoEncontrada.java
│   └── SteamImportacaoException.java
└── AppConfig.java

src/test/java/com/gamelibrary/gamelibrarymanager/
└── service/
    └── SteamServiceStatusTest.java
```

Na raiz do projeto, o `.env.example` é o modelo do `.env` (veja [Configuração da Steam](#configuração-da-steam)).
