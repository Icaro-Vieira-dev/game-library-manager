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
- 🔍 Busca por nome, gênero, plataforma, status e ano de lançamento

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

- ⚡ Rodam **sem PostgreSQL e sem `.env`**: o teste da aplicação inteira usa um banco **H2 em memória**.
- Cobrem a regra de status da importação, a leitura do ano de lançamento e a subida da aplicação (15 testes).
- ✅ Rodam sozinhos no **GitHub Actions** a cada push (o selo **CI** no topo mostra o resultado).

---

## 📡 Endpoints

### 🎮 Jogos — `/jogos`

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/jogos` | Lista todos os jogos |
| POST | `/jogos` | Cadastra um novo jogo |
| GET | `/jogos/{id}` | Busca jogo por ID |
| PUT | `/jogos/{id}` | Atualiza jogo por ID |
| DELETE | `/jogos/{id}` | Remove jogo por ID |
| DELETE | `/jogos/deleteAllJogos` | ⚠️ Remove **todos** os jogos |
| PUT | `/jogos/{id}/platina` | 🏆 Define a meta de platina; em jogos da Steam, busca o progresso automaticamente |
| DELETE | `/jogos/{id}/platina` | Remove a platina e o checklist (o jogo não é apagado) |
| GET | `/jogos/{id}/conquistas?concluida=` | ✅ Checklist de conquistas (`concluida=false` mostra só as que faltam) |
| PUT | `/jogos/{id}/categorias/{categoriaId}` | 🗂️ Coloca o jogo numa categoria (um jogo pode ter várias) |
| DELETE | `/jogos/{id}/categorias/{categoriaId}` | Tira o jogo da categoria (nada é apagado, só o vínculo) |
| GET | `/jogos/buscar/nome?nome=` | 🔍 Busca por nome (parcial, case-insensitive) |
| GET | `/jogos/buscar/genero?genero=` | 🔍 Busca por gênero |
| GET | `/jogos/buscar/plataforma?plataforma=` | 🔍 Busca por plataforma |
| GET | `/jogos/buscar/status?status=` | 🔍 Busca por status |
| GET | `/jogos/buscar/anoLancamento?anoLancamento=` | 🔍 Busca por ano de lançamento |

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

- **Obrigatórios:** `nomeJogo`, `generoJogo`, `plataformaJogo`, `statusJogo`.
- **Opcionais:** `anoLancamentoJogo` (de 1958 até o ano atual + 5), `notaJogo` (de 1 a 10), `desenvolvedoraJogo`. Sem ano ou sem nota, o campo fica `0`.
- `horasJogadas` pode ser `0` (jogo ainda não jogado), mas não pode ser negativo.

**Status disponíveis:** 🎮 `JOGANDO` | ✅ `FINALIZADO` | 💤 `ABANDONADO` | 📋 `PLANEJADO`

---

### 🗂️ Categorias — `/categorias`

As categorias são os **gêneros** dos jogos (Ação, RPG, Aventura…). Um jogo pode estar em várias. Nos jogos da Steam, elas são criadas e ligadas automaticamente pelo [`POST /importSteam/detalhes`](#detalhes-da-loja). Nos outros jogos, você liga à mão com `PUT /jogos/{id}/categorias/{categoriaId}`.

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/categorias` | Lista todas as categorias, com seus jogos |
| POST | `/categorias` | Cria uma categoria |
| POST | `/categorias/manyCategories` | Cria múltiplas categorias |
| GET | `/categorias/{id}` | Busca categoria por ID |
| GET | `/categorias/nome?nome=` | 🔍 Busca categoria por nome |
| DELETE | `/categorias/{id}` | Remove categoria por ID |
| DELETE | `/categorias/deleteAll` | ⚠️ Remove **todas** as categorias |

> 🔒 Uma categoria com jogos **não pode ser apagada**: a API responde `409` dizendo quantos jogos ela tem. Tire os jogos antes com `DELETE /jogos/{id}/categorias/{categoriaId}`.

> 🚫 **Sem nomes repetidos:** criar uma categoria que já existe (`"rpg"` = `"RPG"`) responde `409` com o id da que já existe. O `manyCategories` reaproveita as existentes em vez de duplicar, e se algum nome da lista estiver vazio, **nada** é criado (`400`).

---

### 🔄 Integração Steam — `/importSteam`

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/importSteam/importar` | 📥 Importa a biblioteca da Steam do `STEAM_ID` configurado no `.env` |
| POST | `/importSteam/importar?steamId=` | 📥 Importa a biblioteca de outro perfil |
| POST | `/importSteam/detalhes?limite=` | 🏷️ Completa gênero, categorias, desenvolvedora e ano pela loja da Steam |

**Como usar:** configure o `.env` (veja [Configuração da Steam](#configuracao-da-steam)) e faça a requisição:

```http
POST http://localhost:8080/importSteam/importar
```

**O que a importação faz:**

- ⏱️ Converte o tempo de jogo de minutos para horas.
- 🚫 **Não duplica:** cada jogo guarda o seu `steamAppId`, e os jogos que já foram importados são ignorados. A resposta traz só os jogos novos (`[]` se não houver nada novo).
- 🤖 **Define o status inicial** de cada jogo:

| Situação na Steam | Status |
|---|---|
| Nunca jogou | 📋 `PLANEJADO` |
| Jogou nas últimas 2 semanas | 🎮 `JOGANDO` |
| Não joga há 2 semanas e tem 10h ou mais | ✅ `FINALIZADO` |
| Não joga há 2 semanas e tem menos de 10h | 💤 `ABANDONADO` |

> 💡 O status é uma estimativa baseada nas horas jogadas. Jogos online, ou zerados em poucas horas, podem precisar de ajuste manual com `PUT /jogos/{id}`.

**❌ Erros (422, com mensagem explicando o que fazer):** chave não configurada ou recusada pela Steam, Steam ID fora do formato de 17 dígitos, ou perfil com os detalhes de jogo privados.

✏️ Depois de importar, você pode editar cada jogo para adicionar nota, gênero etc.

<a id="detalhes-da-loja"></a>
#### 🏷️ Detalhes da loja (gênero, categorias, desenvolvedora, ano)

A importação traz só nome e horas. Para completar o resto, rode depois:

```http
POST http://localhost:8080/importSteam/detalhes
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
| `imagemBanner` | horizontal, 460×215 | sempre, depois do `POST /importSteam/detalhes` |
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
│       ├── SteamConquistasResponseDTO.java
│       ├── SteamLojaAppDTO.java
│       └── DetalhesSteamResultadoDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── JogoNaoEncontradoException.java
│   ├── JogoNaoDeletadoException.java
│   ├── CategoriaNaoEncontrada.java
│   ├── CategoriaEmUsoException.java
│   ├── CategoriaDuplicadaException.java
│   ├── RequisicaoInvalidaException.java
│   └── SteamImportacaoException.java
├── AppConfig.java
└── OpenApiConfig.java          (textos do Swagger)

src/test/
├── java/com/gamelibrary/gamelibrarymanager/
│   ├── GameLiabraryManagerApplicationTests.java   (sobe a aplicação com H2)
│   └── service/
│       ├── SteamServiceStatusTest.java
│       └── SteamServiceAnoTest.java
└── resources/
    └── application-test.properties               (H2 em memória)
```

📄 Na raiz do projeto ficam também:
- `.env.example`: o modelo do `.env` (veja [Configuração](#configuracao-da-steam));
- `docker-compose.yml`: o banco PostgreSQL;
- `.github/workflows/ci.yml`: os testes automáticos no GitHub.
