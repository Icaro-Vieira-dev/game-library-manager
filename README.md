# 🎮 Game Library Manager

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?logo=postgresql&logoColor=white)
![Steam API](https://img.shields.io/badge/Steam-API-1b2838?logo=steam&logoColor=white)
[![CI](https://github.com/Icaro-Vieira-dev/game-library-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/Icaro-Vieira-dev/game-library-manager/actions/workflows/ci.yml)

API REST para gerenciar sua biblioteca pessoal de jogos com integração direta à Steam. Em vez de cadastrar jogo por jogo, você conecta sua conta da Steam e importa toda a sua biblioteca automaticamente — jogos jogados, tempo de jogo incluído. 🕹️

## ✨ Funcionalidades

- 📝 Cadastro, edição e remoção de jogos manualmente
- 📥 Importação automática da biblioteca da Steam via API, sem duplicar jogos já importados
- 🤖 Status definido automaticamente na importação, a partir das horas jogadas
- 🗂️ Categorias (gêneros) por jogo, preenchidas automaticamente pela loja da Steam
- 🏷️ Gênero, desenvolvedora e ano de lançamento buscados na Steam
- 🖼️ Imagens dos jogos (banner e capa vertical) prontas para o front-end
- 🚦 Controle de status de cada jogo (jogando, finalizado, abandonado, planejado)
- ⭐ Sistema de avaliação (nota de 1 a 10)
- 🏆 Meta de platina por jogo (opcional), com o progresso de conquistas buscado na Steam
- ✅ Checklist de conquistas: o que já foi feito e o que falta
- 🔍 Busca com filtros combináveis (nome, gênero, plataforma, status, ano, categoria), paginação e ordenação
- ✏️ Edição parcial (`PATCH`): mude só o campo que quiser

## 🛠️ Tecnologias

- ☕ **Java 25** + 🍃 **Spring Boot 4**
- 🗄️ **Spring Data JPA** + **Hibernate**
- 🐘 **PostgreSQL**
- ✔️ **Spring Validation**
- 🌐 **RestTemplate** (consumo da API da Steam)
- 📖 **springdoc-openapi** (documentação interativa com Swagger)
- 🐳 **Docker Compose** (banco de dados)
- 🧪 **JUnit 5** + **H2** (testes) e **GitHub Actions** (CI)

## 📋 Pré-requisitos

- ☕ Java 25+
- 🐳 Docker (para o banco), ou um PostgreSQL rodando em `localhost:5432`
- O Maven não precisa: o projeto usa o `./mvnw`, que já vem junto

<a id="configuracao-da-steam"></a>
## 🔑 Configuração (`.env`)

Tudo o que é segredo fica num arquivo `.env` na raiz do projeto. 🔒 Esse arquivo está no `.gitignore`, então nada dele vai para o GitHub.

1. Copie o modelo: `cp .env.example .env`
2. Preencha os valores, sem aspas e sem espaços:

```
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha
STEAM_API_KEY=sua_chave
STEAM_ID=76561198xxxxxxxxx
```

- 🐘 **DB_USERNAME / DB_PASSWORD:** o usuário e a senha do banco. O Docker Compose cria o banco com esses mesmos valores.
- 🗝️ **STEAM_API_KEY:** [steamcommunity.com/dev/apikey](https://steamcommunity.com/dev/apikey)
- 🆔 **STEAM_ID:** use o SteamID64, com 17 dígitos e começando com `7656`. Ele aparece em [steamid.io](https://steamid.io).

## 🐘 Banco de dados

Com o `.env` preenchido, suba o PostgreSQL com um comando:

```bash
docker compose up -d
```

- O banco `gamelibrary` é criado com o usuário e a senha do `.env`.
- Os dados ficam num volume do Docker: parar ou apagar o container **não** apaga o banco.
- Ele religa sozinho quando o Docker iniciar (`restart: unless-stopped`).
- O Hibernate cria as tabelas automaticamente (`ddl-auto=update`).

## 🚀 Como rodar

```bash
./mvnw spring-boot:run
```

💻 A API sobe em `http://localhost:8080`.

## 📖 Documentação interativa (Swagger)

Com a API rodando, abra **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**: lá estão todos os endpoints, agrupados em **Jogos**, **Categorias** e **Steam**, e dá para **testar cada um direto no navegador** (botão *Try it out*), sem Postman.

O mesmo conteúdo, em formato OpenAPI (JSON), fica em `http://localhost:8080/v3/api-docs`.

## 🧪 Testes

```bash
./mvnw test
```

- ⚡ Rodam **sem PostgreSQL e sem `.env`**: os testes que sobem a aplicação usam um banco **H2 em memória**, e os que falam com a Steam usam um **servidor falso** (`MockRestServiceServer`).
- 56 testes, de dois tipos:
  - **unitários:** regra de status, leitura do ano, mescla da importação, leitura do JSON da Steam;
  - **de integração** (`JogoApiIntegracaoTest`, com MockMvc): chamam os endpoints de verdade (busca, paginação, PATCH, erros, rotas).
- ✅ Rodam sozinhos no **GitHub Actions** a cada push (o selo **CI** no topo mostra o resultado).

---

## 📡 Endpoints

> ❌ **Erros** saem sempre no mesmo formato, o [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc9457) (RFC 9457):
> ```json
> { "type": "about:blank", "title": "Jogo não encontrado", "status": 404, "detail": "Jogo do id: 99 Não encontrado", "instance": "/jogos/99" }
> ```
> Erros de validação trazem também `"erros": { "campo": "mensagem" }`.

### 🎮 Jogos — `/jogos`

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/jogos` | 🔍 Lista os jogos, com **filtros** e **paginação** (veja abaixo) |
| POST | `/jogos` | Cadastra um novo jogo |
| GET | `/jogos/{id}` | Busca jogo por ID |
| PATCH | `/jogos/{id}` | ✏️ Atualiza **só os campos enviados** (ex.: dar nota) |
| PUT | `/jogos/{id}` | Atualiza **todos** os campos do jogo |
| DELETE | `/jogos/{id}` | Remove jogo por ID |
| DELETE | `/jogos?confirmar=true` | ⚠️ Remove **todos** os jogos (sem o `confirmar=true`, responde 400 e não apaga nada) |
| PUT | `/jogos/{id}/platina` | 🏆 Define a meta de platina; em jogos da Steam, busca o progresso automaticamente |
| DELETE | `/jogos/{id}/platina` | Remove a platina e o checklist (o jogo não é apagado) |
| GET | `/jogos/{id}/conquistas?concluida=` | ✅ Checklist de conquistas (`concluida=false` mostra só as que faltam) |
| PUT | `/jogos/{id}/categorias/{categoriaId}` | 🗂️ Coloca o jogo numa categoria (um jogo pode ter várias) |
| DELETE | `/jogos/{id}/categorias/{categoriaId}` | Tira o jogo da categoria (nada é apagado, só o vínculo) |

#### 🔍 Busca e paginação: `GET /jogos`

Todos os filtros são **opcionais** e podem ser **combinados**:

| Parâmetro | Como filtra | Exemplo |
|---|---|---|
| `nome` | contém o texto, sem diferenciar maiúsculas | `?nome=ring` acha "Elden Ring" |
| `genero` | gênero exato, sem diferenciar maiúsculas | `?genero=rpg` |
| `plataforma` | plataforma exata, sem diferenciar maiúsculas | `?plataforma=pc` |
| `status` | `JOGANDO`, `FINALIZADO`, `ABANDONADO` ou `PLANEJADO` | `?status=JOGANDO` |
| `anoLancamento` | ano exato | `?anoLancamento=2022` |
| `categoria` | nome da categoria, sem diferenciar maiúsculas | `?categoria=aventura` |
| `page` | número da página, começando em 0 | `?page=1` |
| `size` | itens por página (padrão 20, máximo 100) | `?size=10` |
| `sort` | ordenação (padrão: `nomeJogo`) | `?sort=horasJogadas,desc` |

```http
GET http://localhost:8080/jogos?genero=rpg&status=JOGANDO&sort=horasJogadas,desc&size=5
```

```json
{
  "conteudo": [ { "idJogo": 5, "nomeJogo": "Baldur's Gate 3", "...": "..." } ],
  "pagina": 0, "tamanho": 5, "totalElementos": 7, "totalPaginas": 2, "ultima": false
}
```

**Body para criar (`POST`) ou substituir (`PUT`) um jogo:**
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

- **Obrigatórios:** `nomeJogo`, `generoJogo`, `plataformaJogo`, `statusJogo`.
- **Opcionais:** `anoLancamentoJogo` (de 1958 até o ano atual + 5), `notaJogo` (de 1 a 10), `horasJogadas`, `desenvolvedoraJogo`. Sem ano, nota ou horas, o campo fica `0`.
- `horasJogadas` pode ser `0` (jogo ainda não jogado), mas não pode ser negativo.

**`PATCH` (só o que mudar):** mande apenas os campos que quer alterar, com as mesmas regras, mas sem nenhum obrigatório:
```json
{ "notaJogo": 9.5, "statusJogo": "FINALIZADO" }
```

**Status disponíveis:** 🎮 `JOGANDO` | ✅ `FINALIZADO` | 💤 `ABANDONADO` | 📋 `PLANEJADO`

---

### 🗂️ Categorias — `/categorias`

As categorias são os **gêneros** dos jogos (Ação, RPG, Aventura…). Um jogo pode estar em várias. Nos jogos da Steam, elas são criadas e ligadas automaticamente pelo [`POST /steam/detalhes`](#detalhes-da-loja). Nos outros jogos, você liga à mão com `PUT /jogos/{id}/categorias/{categoriaId}`.

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/categorias` | Lista todas as categorias, com seus jogos |
| GET | `/categorias?nome=` | 🔍 Só as categorias cujo nome contém o texto |
| POST | `/categorias` | Cria uma categoria |
| POST | `/categorias/lote` | Cria várias categorias de uma vez (`["RPG", "Ação"]`) |
| GET | `/categorias/{id}` | Busca categoria por ID |
| DELETE | `/categorias/{id}` | Remove categoria por ID |
| DELETE | `/categorias?confirmar=true` | ⚠️ Remove **todas** as categorias (sem o `confirmar=true`, responde 400) |

> 🔒 Uma categoria com jogos **não pode ser apagada**: a API responde `409` dizendo quantos jogos ela tem. Tire os jogos antes com `DELETE /jogos/{id}/categorias/{categoriaId}`.

> 🚫 **Sem nomes repetidos:** criar uma categoria que já existe (`"rpg"` = `"RPG"`) responde `409` com o id da que já existe. O `POST /categorias/lote` reaproveita as existentes em vez de duplicar, e se algum nome da lista estiver vazio, **nada** é criado (`400`).

---

### 🔄 Integração Steam — `/steam`

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/steam/importacao` | 📥 Importa a biblioteca da Steam do `STEAM_ID` configurado no `.env` |
| POST | `/steam/importacao?steamId=` | 📥 Importa a biblioteca de outro perfil |
| POST | `/steam/detalhes?limite=` | 🏷️ Completa gênero, categorias, desenvolvedora, ano e banner pela loja da Steam |

**Como usar:** configure o `.env` (veja [Configuração](#configuracao-da-steam)) e faça a requisição:

```http
POST http://localhost:8080/steam/importacao
```

```json
{ "jogosNovos": 3, "horasAtualizadas": 12, "novos": [ { "idJogo": 140, "nomeJogo": "Hades", "...": "..." } ] }
```

**O que a importação faz:**

- ⏱️ Converte o tempo de jogo de minutos para horas.
- 🚫 **Não duplica:** cada jogo guarda o seu `steamAppId`.
- 🔁 **Sincroniza as horas:** nos jogos que **já existem**, atualiza só as horas jogadas. Status, nota e o resto ficam como você deixou. Dá para chamar sempre que quiser.
- 🤖 **Define o status inicial** de cada jogo novo:

| Situação na Steam | Status |
|---|---|
| Nunca jogou | 📋 `PLANEJADO` |
| Jogou nas últimas 2 semanas | 🎮 `JOGANDO` |
| Não joga há 2 semanas e tem 10h ou mais | ✅ `FINALIZADO` |
| Não joga há 2 semanas e tem menos de 10h | 💤 `ABANDONADO` |

> 💡 O status é uma estimativa baseada nas horas jogadas. Jogos online, ou zerados em poucas horas, podem precisar de ajuste manual com `PATCH /jogos/{id}`.

**❌ Erros:** `422` para chave não configurada ou recusada pela Steam, Steam ID fora do formato de 17 dígitos, ou perfil com os detalhes de jogo privados; `504` se a Steam não responder a tempo (limite de 20 s).

✏️ Depois de importar, você pode editar cada jogo para adicionar nota, gênero etc.

<a id="detalhes-da-loja"></a>
#### 🏷️ Detalhes da loja (gênero, categorias, desenvolvedora, ano)

A importação traz só nome e horas. Para completar o resto, rode depois:

```http
POST http://localhost:8080/steam/detalhes
```

```json
{ "processados": 50, "atualizados": 50, "semDadosNaLoja": 0, "restantes": 12, "mensagem": "Ainda faltam 12 jogo(s). Chame de novo para continuar." }
```

- 🗂️ Cada **gênero** da Steam vira uma **categoria** ligada ao jogo (reaproveitada se já existir). O primeiro gênero vira o `generoJogo`.
- 🖼️ Salva a URL do **banner** do jogo (`imagemBanner`).
- ✍️ Só preenche campos **vazios**: o que você editou à mão não é sobrescrito.
- 🐢 A loja da Steam aceita cerca de 200 consultas a cada 5 minutos. Por isso, cada chamada processa até `limite` jogos (padrão 50, máximo 200), com uma pausa entre eles. Se `restantes` for maior que 0, é só chamar de novo.
- 🔓 Não precisa de API key.

---

## 🧩 Modelo de dados

### 🎮 Jogo

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
| imagemBanner | String | URL do banner horizontal (460×215) da Steam |
| imagemCapa | String | URL da capa vertical (600×900) da Steam (veja [Imagens](#imagens)) |
| platina | Platina | Meta de platina e conquistas (opcional; não aparece se não foi definida) |
| categorias | List | Nomes das categorias (gêneros) do jogo, em ordem alfabética |

<a id="imagens"></a>
### 🖼️ Imagens

Os jogos da Steam trazem duas imagens, guardadas como **URL**: as imagens ficam hospedadas na própria Steam.

| Campo | Formato | Quando existe |
|---|---|---|
| `imagemBanner` | horizontal, 460×215 | sempre, depois do `POST /steam/detalhes` |
| `imagemCapa` | vertical, 600×900 (como a capa da biblioteca da Steam) | na maioria dos jogos, mas **não em todos** |

> 💡 **No front-end:** use a `imagemCapa` e, se ela não carregar (404), troque pela `imagemBanner`. Exemplo em HTML:
> ```html
> <img src="{imagemCapa}" onerror="this.onerror=null; this.src='{imagemBanner}'">
> ```

Jogos cadastrados à mão não têm imagem.

### 🏆 Platina / Conquistas

A platina é **opcional**. Ela marca se você tem a meta de platinar o jogo (`platinaAtiva`) e quanto já avançou.

**🔄 Jogos importados da Steam:** basta ativar. A API busca as conquistas na Steam, preenche os números e monta o checklist.

```http
PUT http://localhost:8080/jogos/{id}/platina
{ "platinaAtiva": true }
```

🔁 Para atualizar o progresso depois de desbloquear mais conquistas, é só chamar de novo.

**🎮 Jogos de outras plataformas** (cadastrados à mão): informe os números.

```json
{ "platinaAtiva": true, "conquistaObtida": 45, "totalConquista": 60 }
```

- `platinaAtiva` é obrigatório: `true` ativa a meta, e `false` desiste dela sem apagar o progresso.
- `conquistaObtida` e `totalConquista` são opcionais, mas vão juntos. As obtidas não podem passar do total.
- Se você mandar os números, eles valem mais que os da Steam.

**📊 Na resposta**, a platina ganha dois campos calculados: `conquistaRestante` (quantas faltam) e `platinado` (`true` quando obtidas = total).

**❌ Erros (422):** o jogo não tem conquistas na Steam, ou os detalhes de jogo do seu perfil estão privados.

### ✅ Checklist de conquistas

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
- 🇧🇷 Nomes e descrições vêm em português quando o jogo tem tradução.
- 🤫 Conquistas secretas vêm com a descrição vazia, porque a Steam esconde esse texto.

---

## 📁 Estrutura do projeto

```
src/main/java/com/gamelibrary/gamelibrarymanager/
├── controller/
│   ├── JogoController.java
│   ├── CategoriaController.java
│   └── SteamController.java
├── service/
│   ├── JogoService.java
│   ├── CategoriaService.java
│   └── SteamService.java
├── repository/
│   ├── JogoRepository.java
│   ├── JogoEspecificacoes.java      (filtros da busca GET /jogos)
│   ├── CategoriaRepository.java
│   └── ConquistaRepository.java
├── model/
│   ├── Jogo.java
│   ├── Categoria.java
│   ├── Platina.java
│   ├── Conquista.java
│   └── StatusJogo.java (enum)
├── dto/                             (records)
│   ├── JogoRequestDTO.java
│   ├── JogoPatchDTO.java
│   ├── JogoResponseDTO.java
│   ├── FiltroJogosDTO.java
│   ├── PaginaResponseDTO.java
│   ├── CategoriaRequestDTO.java
│   ├── CategoriaResponseDTO.java
│   ├── PlatinaRequestDTO.java
│   ├── ConquistaResponseDTO.java
│   └── steam/
│       ├── SteamBibliotecaResponseDTO.java
│       ├── SteamJogoDTO.java
│       ├── SteamConquistasResponseDTO.java
│       ├── SteamConquistaDTO.java
│       ├── SteamLojaAppDTO.java
│       ├── ImportacaoSteamResultadoDTO.java
│       └── DetalhesSteamResultadoDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java  (todos os erros em ProblemDetail)
│   ├── JogoNaoEncontradoException.java
│   ├── CategoriaNaoEncontradaException.java
│   ├── CategoriaEmUsoException.java
│   ├── CategoriaDuplicadaException.java
│   ├── RequisicaoInvalidaException.java
│   └── SteamImportacaoException.java
├── AppConfig.java                   (RestClient da Steam, com tempo-limite)
├── OpenApiConfig.java               (textos do Swagger)
└── GameLibraryManagerApplication.java

src/test/
├── java/com/gamelibrary/gamelibrarymanager/
│   ├── GameLibraryManagerApplicationTests.java   (sobe a aplicação com H2)
│   ├── controller/
│   │   └── JogoApiIntegracaoTest.java            (endpoints com MockMvc + H2)
│   ├── dto/steam/
│   │   └── SteamDtoJsonTest.java                 (leitura do JSON da Steam)
│   └── service/
│       ├── SteamServiceStatusTest.java
│       ├── SteamServiceAnoTest.java
│       ├── SteamServiceMesclaTest.java
│       └── SteamServiceHttpTest.java             (Steam falsa com MockRestServiceServer)
└── resources/
    └── application-test.properties               (H2 em memória)
```

📄 Na raiz do projeto ficam também:
- `.env.example`: o modelo do `.env` (veja [Configuração](#configuracao-da-steam));
- `docker-compose.yml`: o banco PostgreSQL;
- `.github/workflows/ci.yml`: os testes automáticos no GitHub.
