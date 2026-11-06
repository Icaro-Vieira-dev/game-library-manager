# Rotas da API — Game Library

Base: `http://localhost:8080`

- 🔓 **Pública**: funciona sem login.
- 🔒 **Exige login**: precisa do cookie `token`, que o `POST /auth/login` cria (o Postman guarda sozinho). Sem login, a resposta é **401**.
- Nas rotas 🔒 você só vê e mexe nos **seus** dados. Jogo ou categoria de outra pessoa responde **404**, como se não existisse.
- Todos os erros vêm no formato ProblemDetail: `{ "title", "status", "detail" }`. Nos erros de validação (400) vem também `"erros": { "campo": "mensagem" }`.

---

## Autenticação

### 🔓 POST `/auth/cadastro` — criar conta
```json
{
  "nome": "seu nome",
  "email": "example@email.com",
  "senha": "uma-senha-forte",
  "steamId": "76561198000000000"
}
```
- `nome`: obrigatório, até 100 caracteres.
- `email`: válido, até 254 caracteres.
- `senha`: de 8 a 128 caracteres.
- `steamId`: opcional; se vier, são 17 dígitos começando com `7656119`.

Respostas:
- **201**: `{ id, nome, email, steamId, criadoEm }`.
- **400**: campo inválido.
- **409**: e-mail ou Steam ID já usado.

### 🔓 POST `/auth/login` — entrar
```json
{ "email": "icaro@email.com", "senha": "uma-senha-forte" }
```
Respostas:
- **200**: `{ "expiraEm": "..." }` e o cookie `token` (vale 60 min). O token **não** vem no corpo.
- **401**: e-mail ou senha inválidos.
- **429**: 5 erros em 15 min (o cabeçalho `Retry-After` diz quando tentar de novo).

### 🔓 POST `/auth/logout` — sair
Sem body.
- **204**: o cookie `token` é apagado.

---

## Usuário

### 🔒 GET `/usuarios/me` — quem está logado
- **200**: `{ id, nome, email, steamId, criadoEm }`.

### 🔒 PATCH `/usuarios/me` — trocar o Steam ID
```json
{ "steamId": "76561198000000000" }
```
- `{}`, `null` ou `""` **removem** o Steam ID.

Respostas:
- **200**: o usuário atualizado.
- **400**: formato inválido.
- **409**: o Steam ID já está em outra conta.

---

## Jogos

### 🔒 GET `/jogos` — listar (com filtros e paginação)
Todos os filtros são opcionais e podem ser combinados:

| Parâmetro | Exemplo | O que faz |
|---|---|---|
| `nome` | `?nome=ring` | parte do nome (não diferencia maiúsculas) |
| `genero` | `?genero=rpg` | gênero exato |
| `plataforma` | `?plataforma=pc` | plataforma exata |
| `status` | `?status=JOGANDO` | `JOGANDO`, `FINALIZADO`, `ABANDONADO`, `PLANEJADO` |
| `anoLancamento` | `?anoLancamento=2022` | ano exato |
| `categoria` | `?categoria=Ação` | nome da categoria |
| `page` | `?page=1` | página, começando em **0** |
| `size` | `?size=50` | itens por página (padrão 20, máximo 100) |
| `sort` | `?sort=horasJogadas,desc` | ordenação (padrão: nome) |

Exemplos:
```
GET /jogos
GET /jogos?status=JOGANDO&genero=rpg
GET /jogos?sort=horasJogadas,desc&size=10
```
- **200**: `{ conteudo: [...], pagina, tamanho, totalElementos, totalPaginas, ultima }`.

### 🔒 GET `/jogos/{id}` — um jogo
- **200**: o jogo.
- **404**: não existe ou é de outra pessoa.

### 🔒 POST `/jogos` — cadastrar à mão
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
- Obrigatórios: `nomeJogo`, `generoJogo`, `plataformaJogo`, `statusJogo`.
- `anoLancamentoJogo`: de 1958 até o ano atual + 5.
- `notaJogo`: de 1 a 10.
- `horasJogadas`: 0 ou mais.

Respostas:
- **200**: o jogo criado (com `idJogo`).
- **400**: campo inválido.

### 🔒 PATCH `/jogos/{id}` — mudar só alguns campos
```json
{ "statusJogo": "FINALIZADO", "notaJogo": 9.5 }
```
- **200**: o jogo atualizado.

### 🔒 PUT `/jogos/{id}` — substituir o jogo inteiro
O body é o mesmo do `POST /jogos`. Campo que não for enviado fica vazio ou 0.
- **200**: o jogo atualizado.

### 🔒 DELETE `/jogos/{id}` — apagar um jogo
- **204**: apagado.
- **404**: não existe ou é de outra pessoa.

### 🔒 DELETE `/jogos?confirmar=true` — apagar TODOS os seus jogos
- **204**: apagados.
- **400**: faltou o `?confirmar=true`. Não tem como desfazer.

### 🔒 PUT `/jogos/{id}/platina` — definir a platina
```json
{ "platinaAtiva": true }
```
Busca o progresso na Steam (precisa de Steam ID na conta). Para informar à mão (ex.: jogo de console):
```json
{ "platinaAtiva": true, "conquistaObtida": 45, "totalConquista": 60 }
```
Respostas:
- **200**: o jogo, com `conquistaRestante` e `platinado`.
- **422**: o jogo não tem conquistas, ou as conquistas estão privadas.

### 🔒 DELETE `/jogos/{id}/platina` — remover a platina
- **200**: o jogo, sem o campo `platina`.

### 🔒 GET `/jogos/{id}/conquistas` — checklist de conquistas
```
GET /jogos/5/conquistas
GET /jogos/5/conquistas?concluida=false
GET /jogos/5/conquistas?concluida=true
```
- `?concluida=false`: só as que faltam.
- `?concluida=true`: só as feitas.
- **200**: `[ { nome, descricao, concluida, dataDesbloqueio } ]`.

### 🔒 PUT `/jogos/{id}/categorias/{categoriaId}` — ligar o jogo a uma categoria
Sem body.
- **200**: o jogo com a categoria.
- **404**: o jogo ou a categoria não é sua.

### 🔒 DELETE `/jogos/{id}/categorias/{categoriaId}` — desligar o jogo da categoria
- **200**: o jogo sem essa categoria (a categoria não é apagada).

---

## Categorias

### 🔒 GET `/categorias` — listar
```
GET /categorias
GET /categorias?nome=rp
```
- `?nome=rp`: só as que contêm "rp".
- **200**: `[ { idCategoria, nome, jogos: [...] } ]`.

### 🔒 GET `/categorias/{id}` — uma categoria
- **200**: a categoria e os seus jogos nela.
- **404**: não existe ou é de outra pessoa.

### 🔒 POST `/categorias` — criar
```json
{ "nome": "RPG" }
```
- **200**: a categoria criada.
- **409**: já existe ("rpg" = "RPG").

### 🔒 POST `/categorias/lote` — criar várias
```json
["RPG", "Ação", "Estratégia"]
```
- **200**: as categorias, sem repetir as que já existem.

### 🔒 DELETE `/categorias/{id}` — apagar uma
- **204**: apagada.
- **409**: ainda tem jogos nela.

### 🔒 DELETE `/categorias?confirmar=true` — apagar TODAS as suas
- **204**: apagadas.
- **400**: faltou o `?confirmar=true`.
- **409**: algum jogo ainda está em alguma categoria.

---

## Steam

### 🔒 POST `/steam/importacao` — importar a sua biblioteca
Sem body. Usa o Steam ID **da sua conta**; não existe `?steamId=`.

Respostas:
- **200**: `{ jogosNovos, horasAtualizadas, novos: [...] }`.
- **422**: sem Steam ID na conta, perfil privado ou problema com a chave da Steam.

Pode chamar sempre que quiser atualizar as horas: jogos já importados não duplicam.

### 🔒 POST `/steam/detalhes` — completar gênero, categorias, desenvolvedora e ano
```
POST /steam/detalhes
POST /steam/detalhes?limite=10
```
- Processa até `limite` jogos por chamada (padrão 50, máximo 200), com 1,5 s entre cada um.
- **200**: `{ processados, atualizados, semDadosNaLoja, restantes, mensagem }`.

Se `restantes` > 0, chame de novo.

---

## Vitrine

### 🔓 GET `/vitrine` — lançamentos e "em breve" da loja da Steam
- **200**: `{ lancamentos: [...], emBreve: [...], atualizadoEm }`.
- Cada jogo vem com `{ appId, nome, imagem, preco, precoOriginal, desconto, dataLancamento, urlLoja }`.
- Atualizada a cada 6 horas.
- Logo depois de a API subir, as listas podem vir vazias com `atualizadoEm: null`.

---

## Ordem para testar do zero (Postman)

1. `POST /auth/cadastro`
2. `POST /auth/login`
3. `PATCH /usuarios/me` com o seu Steam ID (se não informou no cadastro)
4. `POST /steam/importacao`
5. `POST /steam/detalhes` (repita até `restantes` = 0)
6. `GET /jogos?size=100`
7. `POST /auth/logout`
