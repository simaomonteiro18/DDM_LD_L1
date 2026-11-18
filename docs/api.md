# API do iShopping

Documentação dos endpoints do backend, para a construção dos ecrãs da aplicação móvel. Tudo o que está aqui foi tirado do código do backend.

## Índice

1. [Introdução](#1-introdução)
2. [Erros](#2-erros)
3. [Autenticação](#3-autenticação)
4. [Utilizadores](#4-utilizadores)
5. [Tipos de artigo](#5-tipos-de-artigo)
6. [Artigos](#6-artigos)
7. [Orçamentos](#7-orçamentos)
8. [Compras](#8-compras)
9. [Itens de compra](#9-itens-de-compra)
10. [Regras de negócio](#10-regras-de-negócio)
11. [Fluxo de exemplo](#11-fluxo-de-exemplo)

## 1. Introdução

- **URL base:** `http://IP:8080/api`, em que `IP` é o endereço da máquina onde corre o backend. No telemóvel ou no emulador não se pode usar `localhost`.
- **Formato:** pedidos e respostas em JSON. Os pedidos com corpo levam o cabeçalho `Content-Type: application/json`.
- **Autenticação:** todos os endpoints exigem um token, exceto `POST /api/auth/login`. O token obtém-se no login e envia-se em todos os pedidos seguintes:

  ```
  Authorization: Bearer TOKEN
  ```

- **Validade do token:** 8 horas por omissão (variável `JWT_EXPIRACAO_HORAS` do `.env`). A resposta do login traz o momento exato em `expiraEm`. Depois disso, qualquer pedido dá 401 e é preciso fazer login outra vez. Não existe endpoint de renovação.
- **Permissões:** todos os utilizadores têm as mesmas permissões.
- **Datas:** strings ISO 8601 em UTC, por exemplo `"2026-11-12T13:22:45.872238Z"`. O número de casas decimais dos segundos pode variar.
- **Valores monetários** (`valor`, `precoUnitario`): números JSON com até 2 casas decimais, por exemplo `300.50`.
- **Números inteiros** (ids, quantidades, ano, mês): têm de ser inteiros. Um decimal, mesmo `1.0`, dá 400.
- **Campos sem valor:** vêm na resposta como `null`, não são omitidos.
- **Texto:** os espaços no início e no fim dos campos `nome`, `username` e `observacoes` são removidos antes de guardar.
- **Criação:** os `POST` que criam registos devolvem `201 Created` com o cabeçalho `Location`, que tem o endereço do novo registo.
- **`PUT`:** substitui o registo todo. É preciso enviar todos os campos, não só os que mudam. Um campo opcional omitido fica a `null`, exceto a `password` dos utilizadores.

## 2. Erros

Todos os erros usam o mesmo formato:

| Campo | Tipo | Descrição |
|---|---|---|
| `timestamp` | string (data) | Momento do erro |
| `estado` | número | Código HTTP |
| `erro` | string | Mensagem em português, pronta a mostrar ao utilizador |
| `caminho` | string | Caminho do pedido |

Exemplo:

```json
{
  "timestamp": "2026-11-12T13:22:46.707417Z",
  "estado": 409,
  "erro": "Já existe um orçamento para este mês e ano.",
  "caminho": "/api/orcamentos"
}
```

Quando há vários erros de validação no mesmo pedido, as mensagens vêm todas no campo `erro`, separadas por um espaço e por ordem alfabética, não pela ordem dos campos. Por exemplo: `"O ano é obrigatório. O mês é obrigatório. O valor é obrigatório."`

| Código | Significado neste projeto |
|---|---|
| 400 | Pedido inválido: um campo falha a validação, há um campo obrigatório em falta, o JSON está mal formado, um id ou filtro não é do tipo certo, ou há uma regra de negócio violada no pedido (ver itens de compra). As validações dos campos são feitas antes de procurar os registos, por isso um pedido com campos inválidos dá 400 mesmo que o id não exista. A exceção são as regras do `PUT` de itens que dependem do tipo do item (secção 9), verificadas depois de encontrar o item. |
| 401 | Token em falta, inválido ou expirado (`"Autenticação necessária. O token está em falta, é inválido ou expirou."`), ou credenciais erradas no login (`"Credenciais inválidas."`). |
| 404 | O registo não existe. Também se usa quando um registo referido no corpo não existe, por exemplo o tipo de artigo de um artigo, e quando um item é pedido através de uma compra que não é a sua. |
| 409 | Conflito com o estado dos dados: duplicado de um campo único, compra fechada, ou apagar algo que tem registos associados. |

Outros códigos possíveis, que a aplicação não deve precisar de tratar em particular:

| Código | Mensagem |
|---|---|
| 403 | `"Não tem permissão para aceder a este recurso."` |
| 404 | `"O recurso pedido não existe."` (endereço que não existe na API) |
| 405 | `"O método HTTP não é permitido neste endereço."` |
| 415 | `"O tipo de conteúdo do pedido não é suportado."` (falta o `Content-Type: application/json`) |
| 500 | `"Ocorreu um erro interno. Tente novamente mais tarde."` |

## 3. Autenticação

### `POST /api/auth/login`

Valida o username e a palavra-passe e devolve um token. É o único endpoint que não precisa de token.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `username` | string | sim | `"O username é obrigatório."` Não distingue maiúsculas de minúsculas. |
| `password` | string | sim | `"A palavra-passe é obrigatória."` |

**Resposta 200**

| Campo | Tipo | Descrição |
|---|---|---|
| `token` | string | Token a enviar no cabeçalho `Authorization` |
| `tipo` | string | Sempre `"Bearer"` |
| `expiraEm` | string (data) | Momento em que o token deixa de ser válido |
| `utilizador` | objeto | Utilizador autenticado: `id`, `nome`, `username` |

**Códigos:** 200, 400 (campos em falta), 401 `"Credenciais inválidas."` (username inexistente ou palavra-passe errada; a mensagem é a mesma nos dois casos).

```json
POST /api/auth/login
{
  "username": "ana.silva",
  "password": "palavra-passe-exemplo"
}
```

```json
200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZXhwIjoxNzk0NTMwMjQ1fQ.exemplo",
  "tipo": "Bearer",
  "expiraEm": "2026-11-12T21:22:45Z",
  "utilizador": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" }
}
```

## 4. Utilizadores

Objeto utilizador na resposta:

| Campo | Tipo |
|---|---|
| `id` | número |
| `nome` | string |
| `username` | string |

A palavra-passe nunca é devolvida.

### `GET /api/utilizadores`

Lista todos os utilizadores, por nome.

**Códigos:** 200.

```json
200 OK
[
  { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  { "id": 2, "nome": "Rui Costa", "username": "rui.costa" }
]
```

### `GET /api/utilizadores/{id}`

Devolve um utilizador.

**Códigos:** 200, 400 (id não numérico), 404 `"O utilizador não existe."`

### `POST /api/utilizadores`

Cria um utilizador.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `nome` | string | sim | `"O nome é obrigatório."`; máximo 100 caracteres: `"O nome não pode ter mais de 100 caracteres."` |
| `username` | string | sim | `"O username é obrigatório."`; máximo 50 caracteres: `"O username não pode ter mais de 50 caracteres."`; sem espaços: `"O username não pode conter espaços."`; único sem distinguir maiúsculas (409). |
| `password` | string | sim | `"A palavra-passe é obrigatória."`; entre 6 e 72 caracteres e não só espaços: `"A palavra-passe tem de ter entre 6 e 72 caracteres (os caracteres acentuados podem contar como dois) e não pode conter apenas espaços."` |

**Resposta 201:** o utilizador criado, com cabeçalho `Location`.

**Códigos:** 201, 400, 409 `"Já existe um utilizador com este username."`

```json
POST /api/utilizadores
{
  "nome": "Rui Costa",
  "username": "rui.costa",
  "password": "palavra-passe-exemplo"
}
```

```json
201 Created
Location: http://IP:8080/api/utilizadores/2
{ "id": 2, "nome": "Rui Costa", "username": "rui.costa" }
```

### `PUT /api/utilizadores/{id}`

Altera um utilizador.

**Pedido:** os mesmos campos e regras do `POST`, exceto `password`, que é opcional. Se for omitida, `null` ou `""`, a palavra-passe atual mantém-se. Se for enviada, tem de cumprir a mesma regra do `POST`.

**Códigos:** 200, 400, 404 `"O utilizador não existe."`, 409 `"Já existe um utilizador com este username."` (o próprio utilizador pode manter o seu username).

```json
PUT /api/utilizadores/2
{ "nome": "Rui Costa Santos", "username": "rui.costa" }
```

```json
200 OK
{ "id": 2, "nome": "Rui Costa Santos", "username": "rui.costa" }
```

### `DELETE /api/utilizadores/{id}`

Apaga um utilizador.

**Códigos:** 204 (sem corpo), 404 `"O utilizador não existe."`, 409:

- `"Não é possível apagar o próprio utilizador autenticado."`
- `"Não é possível apagar um utilizador com registos associados."` (criou ou alterou compras ou orçamentos)

## 5. Tipos de artigo

Objeto tipo de artigo na resposta:

| Campo | Tipo |
|---|---|
| `id` | número |
| `nome` | string |

### `GET /api/tipos-artigo`

Lista todos os tipos de artigo, por nome.

**Códigos:** 200.

```json
200 OK
[
  { "id": 2, "nome": "Limpeza" },
  { "id": 1, "nome": "Mercearia" }
]
```

### `GET /api/tipos-artigo/{id}`

Devolve um tipo de artigo.

**Códigos:** 200, 400, 404 `"O tipo de artigo não existe."`

### `POST /api/tipos-artigo`

Cria um tipo de artigo.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `nome` | string | sim | `"O nome é obrigatório."`; `"O nome não pode ter mais de 100 caracteres."`; único sem distinguir maiúsculas (409). |

**Códigos:** 201 (com `Location`), 400, 409 `"Já existe um tipo de artigo com este nome."`

```json
POST /api/tipos-artigo
{ "nome": "Bebidas" }
```

```json
201 Created
Location: http://IP:8080/api/tipos-artigo/7
{ "id": 7, "nome": "Bebidas" }
```

### `PUT /api/tipos-artigo/{id}`

Altera o nome de um tipo de artigo.

**Pedido:** igual ao `POST`.

**Códigos:** 200, 400, 404 `"O tipo de artigo não existe."`, 409 `"Já existe um tipo de artigo com este nome."`

```json
PUT /api/tipos-artigo/7
{ "nome": "Bebidas e sumos" }
```

```json
200 OK
{ "id": 7, "nome": "Bebidas e sumos" }
```

### `DELETE /api/tipos-artigo/{id}`

Apaga um tipo de artigo.

**Códigos:** 204, 404 `"O tipo de artigo não existe."`, 409 `"Não é possível apagar um tipo de artigo com artigos associados."`

## 6. Artigos

Objeto artigo na resposta:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | número | |
| `nome` | string | |
| `tipo` | objeto | Tipo de artigo: `id`, `nome` |

### `GET /api/artigos`

Lista os artigos, por nome.

**Parâmetro opcional:** `?idTipoArtigo=N`, para mostrar só os artigos desse tipo. Um tipo inexistente dá uma lista vazia, não 404. Um valor não numérico dá 400.

**Códigos:** 200, 400.

```json
GET /api/artigos?idTipoArtigo=1

200 OK
[
  { "id": 1, "nome": "Arroz", "tipo": { "id": 1, "nome": "Mercearia" } },
  { "id": 3, "nome": "Azeite", "tipo": { "id": 1, "nome": "Mercearia" } }
]
```

### `GET /api/artigos/{id}`

Devolve um artigo.

**Códigos:** 200, 400, 404 `"O artigo não existe."`

### `POST /api/artigos`

Cria um artigo.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `nome` | string | sim | `"O nome é obrigatório."`; `"O nome não pode ter mais de 100 caracteres."`; único dentro do mesmo tipo, sem distinguir maiúsculas (409). |
| `idTipoArtigo` | número inteiro | sim | `"O tipo de artigo é obrigatório."`; tem de existir (404). |

**Códigos:** 201 (com `Location`), 400, 404 `"O tipo de artigo não existe."`, 409 `"Já existe um artigo com este nome neste tipo de artigo."`

```json
POST /api/artigos
{ "nome": "Leite", "idTipoArtigo": 1 }
```

```json
201 Created
Location: http://IP:8080/api/artigos/16
{ "id": 16, "nome": "Leite", "tipo": { "id": 1, "nome": "Mercearia" } }
```

### `PUT /api/artigos/{id}`

Altera o nome e o tipo de um artigo.

**Pedido:** igual ao `POST`. Mudar de tipo verifica a unicidade do nome no tipo de destino.

**Códigos:** 200, 400, 404 (`"O artigo não existe."` ou `"O tipo de artigo não existe."`), 409 `"Já existe um artigo com este nome neste tipo de artigo."`

```json
PUT /api/artigos/16
{ "nome": "Leite meio-gordo", "idTipoArtigo": 1 }
```

```json
200 OK
{ "id": 16, "nome": "Leite meio-gordo", "tipo": { "id": 1, "nome": "Mercearia" } }
```

### `DELETE /api/artigos/{id}`

Apaga um artigo.

**Códigos:** 204, 404 `"O artigo não existe."`, 409 `"Não é possível apagar um artigo com itens de compra associados."`

## 7. Orçamentos

Um orçamento é o valor previsto para gastar num mês.

Objeto orçamento na resposta:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | número | |
| `ano` | número | |
| `mes` | número | 1 a 12 |
| `valor` | número | Com 2 casas decimais |
| `criadoPor` | objeto | Utilizador que criou: `id`, `nome`, `username` |
| `dataCriacao` | string (data) | |
| `alteradoPor` | objeto ou `null` | Último utilizador que alterou; `null` se nunca foi alterado |
| `dataAlterado` | string (data) ou `null` | |

### `GET /api/orcamentos`

Lista os orçamentos, do mais recente para o mais antigo (ano e mês descendentes).

**Parâmetro opcional:** `?ano=AAAA`, só os orçamentos desse ano. Um ano sem orçamentos dá uma lista vazia. Um valor não numérico dá 400.

**Códigos:** 200, 400.

```json
GET /api/orcamentos?ano=2026

200 OK
[
  {
    "id": 2,
    "ano": 2026,
    "mes": 12,
    "valor": 350.00,
    "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
    "dataCriacao": "2026-11-12T13:22:46.045739Z",
    "alteradoPor": null,
    "dataAlterado": null
  }
]
```

### `GET /api/orcamentos/{id}`

Devolve um orçamento.

**Códigos:** 200, 400, 404 `"O orçamento não existe."`

### `POST /api/orcamentos`

Cria um orçamento. `criadoPor` e `dataCriacao` são preenchidos pelo servidor.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `ano` | número inteiro | sim | `"O ano é obrigatório."`; entre 2000 e 2100: `"O ano tem de estar entre 2000 e 2100."` |
| `mes` | número inteiro | sim | `"O mês é obrigatório."`; entre 1 e 12: `"O mês tem de estar entre 1 e 12."` |
| `valor` | número | sim | `"O valor é obrigatório."`; não negativo: `"O valor não pode ser negativo."`; até 8 algarismos inteiros e 2 casas decimais: `"O valor pode ter no máximo 8 algarismos inteiros e 2 casas decimais."` |

O par (`ano`, `mes`) é único (409).

**Códigos:** 201 (com `Location`), 400, 409 `"Já existe um orçamento para este mês e ano."`

```json
POST /api/orcamentos
{ "ano": 2026, "mes": 12, "valor": 350 }
```

```json
201 Created
Location: http://IP:8080/api/orcamentos/2
{
  "id": 2,
  "ano": 2026,
  "mes": 12,
  "valor": 350.00,
  "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataCriacao": "2026-11-12T13:22:46.045739Z",
  "alteradoPor": null,
  "dataAlterado": null
}
```

### `PUT /api/orcamentos/{id}`

Altera um orçamento. `alteradoPor` e `dataAlterado` são preenchidos pelo servidor.

**Pedido:** igual ao `POST`. O próprio orçamento pode manter o seu ano e mês.

**Códigos:** 200, 400, 404 `"O orçamento não existe."`, 409 `"Já existe um orçamento para este mês e ano."`

```json
PUT /api/orcamentos/2
{ "ano": 2026, "mes": 12, "valor": 400 }
```

```json
200 OK
{
  "id": 2,
  "ano": 2026,
  "mes": 12,
  "valor": 400.00,
  "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataCriacao": "2026-11-12T13:22:46.045739Z",
  "alteradoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataAlterado": "2026-11-12T13:30:10.512300Z"
}
```

### `DELETE /api/orcamentos/{id}`

Apaga um orçamento.

**Códigos:** 204, 404 `"O orçamento não existe."`. O código também prevê 409 `"Não é possível apagar um orçamento com registos associados."`, mas hoje nenhuma tabela depende dos orçamentos, por isso esse caso não acontece.

## 8. Compras

Objeto compra na resposta:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | número | |
| `nome` | string | |
| `fechada` | booleano | |
| `criadoPor` | objeto | Utilizador que criou: `id`, `nome`, `username` |
| `dataCriacao` | string (data) | |
| `alteradoPor` | objeto ou `null` | Último utilizador que alterou o nome |
| `dataAlterado` | string (data) ou `null` | |
| `fechadaPor` | objeto ou `null` | Utilizador que fechou; `null` enquanto estiver aberta |
| `dataFechada` | string (data) ou `null` | |

Os itens não vêm na compra. Obtêm-se em `GET /api/compras/{id}/itens`.

### `GET /api/compras`

Lista as compras, da mais recente para a mais antiga (data de criação).

**Parâmetro opcional:** `?fechada=true` (só fechadas) ou `?fechada=false` (só abertas). Um valor que não seja booleano dá 400.

**Códigos:** 200, 400.

```json
GET /api/compras?fechada=false

200 OK
[
  {
    "id": 5,
    "nome": "Compras da semana",
    "fechada": false,
    "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
    "dataCriacao": "2026-11-12T13:38:20.272588Z",
    "alteradoPor": null,
    "dataAlterado": null,
    "fechadaPor": null,
    "dataFechada": null
  }
]
```

### `GET /api/compras/{id}`

Devolve uma compra.

**Códigos:** 200, 400, 404 `"A compra não existe."`

### `POST /api/compras`

Cria uma compra, aberta. `criadoPor` e `dataCriacao` são preenchidos pelo servidor.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `nome` | string | sim | `"O nome é obrigatório."`; `"O nome não pode ter mais de 100 caracteres."`. Não é único. |

**Códigos:** 201 (com `Location`), 400.

```json
POST /api/compras
{ "nome": "Compras da semana" }
```

```json
201 Created
Location: http://IP:8080/api/compras/5
{
  "id": 5,
  "nome": "Compras da semana",
  "fechada": false,
  "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataCriacao": "2026-11-12T13:38:20.272588Z",
  "alteradoPor": null,
  "dataAlterado": null,
  "fechadaPor": null,
  "dataFechada": null
}
```

### `PUT /api/compras/{id}`

Altera o nome de uma compra aberta. `alteradoPor` e `dataAlterado` são preenchidos pelo servidor.

**Pedido:** igual ao `POST`.

**Códigos:** 200, 400, 404 `"A compra não existe."`, 409 `"Não é possível alterar uma compra fechada."`

```json
PUT /api/compras/5
{ "nome": "Compras de sábado" }
```

```json
200 OK
{
  "id": 5,
  "nome": "Compras de sábado",
  "fechada": false,
  "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataCriacao": "2026-11-12T13:38:20.272588Z",
  "alteradoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataAlterado": "2026-11-12T13:40:02.104512Z",
  "fechadaPor": null,
  "dataFechada": null
}
```

### `POST /api/compras/{id}/fechar`

Fecha uma compra. Não tem corpo. `fechadaPor` e `dataFechada` são preenchidos pelo servidor. Depois de fechada, a compra e os seus itens já não podem ser alterados nem apagados.

**Códigos:** 200 (a compra fechada), 400, 404 `"A compra não existe."`, 409 `"A compra já está fechada."`

```json
POST /api/compras/5/fechar
```

```json
200 OK
{
  "id": 5,
  "nome": "Compras de sábado",
  "fechada": true,
  "criadoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataCriacao": "2026-11-12T13:38:20.272588Z",
  "alteradoPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataAlterado": "2026-11-12T13:40:02.104512Z",
  "fechadaPor": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" },
  "dataFechada": "2026-11-12T18:05:41.230001Z"
}
```

### `DELETE /api/compras/{id}`

Apaga uma compra aberta e todos os seus itens.

**Códigos:** 204, 404 `"A compra não existe."`, 409 `"Não é possível apagar uma compra fechada."`

## 9. Itens de compra

Cada item pertence a uma compra e a um artigo. Há dois tipos:

- **Previsto:** faz parte da lista do que se quer comprar. Tem uma quantidade prevista e pode ser marcado como adquirido mais tarde, com a quantidade adquirida e o preço.
- **Não previsto:** comprado sem estar na lista. Entra já adquirido, com quantidade e preço obrigatórios, e pode ter observações.

O tipo escolhe-se no endpoint de criação e não muda depois.

Objeto item na resposta:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | número | |
| `tipo` | string | `"PREVISTO"` ou `"NAO_PREVISTO"` |
| `artigo` | objeto | Artigo com o seu tipo: `id`, `nome`, `tipo` { `id`, `nome` } |
| `quantidadePrevista` | número ou `null` | Só nos previstos; `null` nos não previstos |
| `quantidadeAdquirida` | número ou `null` | `null` num previsto ainda não adquirido |
| `precoUnitario` | número ou `null` | Com 2 casas decimais; `null` quando `quantidadeAdquirida` é `null` |
| `observacoes` | string ou `null` | Só nos não previstos; `null` nos previstos |

A `quantidadeAdquirida` e o `precoUnitario` estão sempre os dois preenchidos ou os dois a `null`.

Todos os caminhos começam por `/api/compras/{idCompra}/itens`. Um item só se encontra através da sua compra: se o item existir mas pertencer a outra compra, a resposta é 404 `"O item de compra não existe."`

### `GET /api/compras/{idCompra}/itens`

Lista os itens da compra, dos dois tipos, pela ordem em que foram criados. Funciona também em compras fechadas.

**Códigos:** 200, 400, 404 `"A compra não existe."`

```json
200 OK
[
  {
    "id": 2,
    "tipo": "PREVISTO",
    "artigo": { "id": 1, "nome": "Arroz", "tipo": { "id": 1, "nome": "Mercearia" } },
    "quantidadePrevista": 2,
    "quantidadeAdquirida": null,
    "precoUnitario": null,
    "observacoes": null
  },
  {
    "id": 4,
    "tipo": "NAO_PREVISTO",
    "artigo": { "id": 3, "nome": "Azeite", "tipo": { "id": 1, "nome": "Mercearia" } },
    "quantidadePrevista": null,
    "quantidadeAdquirida": 2,
    "precoUnitario": 1.25,
    "observacoes": "Estava em promoção"
  }
]
```

### `GET /api/compras/{idCompra}/itens/{idItem}`

Devolve um item da compra.

**Códigos:** 200, 400, 404 (`"A compra não existe."` ou `"O item de compra não existe."`)

### `POST /api/compras/{idCompra}/itens/previstos`

Adiciona um item previsto a uma compra aberta.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `idArtigo` | número inteiro | sim | `"O artigo é obrigatório."`; tem de existir (404 `"O artigo não existe."`) |
| `quantidadePrevista` | número inteiro | sim | `"A quantidade prevista é obrigatória."`; `"A quantidade prevista tem de ser maior que zero."` |
| `quantidadeAdquirida` | número inteiro | não | `"A quantidade adquirida tem de ser maior que zero."` |
| `precoUnitario` | número | não | `"O preço unitário não pode ser negativo."`; `"O preço unitário pode ter no máximo 8 algarismos inteiros e 2 casas decimais."` |

Se vier um dos dois campos `quantidadeAdquirida` e `precoUnitario`, tem de vir o outro: `"A quantidade adquirida e o preço unitário têm de ser indicados em conjunto."` O mesmo artigo pode aparecer mais do que uma vez na mesma compra.

**Códigos:** 201 (com `Location`), 400, 404 (`"A compra não existe."` ou `"O artigo não existe."`), 409 `"Não é possível adicionar itens a uma compra fechada."`

```json
POST /api/compras/5/itens/previstos
{ "idArtigo": 1, "quantidadePrevista": 2 }
```

```json
201 Created
Location: http://IP:8080/api/compras/5/itens/2
{
  "id": 2,
  "tipo": "PREVISTO",
  "artigo": { "id": 1, "nome": "Arroz", "tipo": { "id": 1, "nome": "Mercearia" } },
  "quantidadePrevista": 2,
  "quantidadeAdquirida": null,
  "precoUnitario": null,
  "observacoes": null
}
```

### `POST /api/compras/{idCompra}/itens/nao-previstos`

Adiciona um item não previsto, já adquirido, a uma compra aberta.

**Pedido**

| Campo | Tipo | Obrigatório | Regras e mensagens |
|---|---|---|---|
| `idArtigo` | número inteiro | sim | `"O artigo é obrigatório."`; tem de existir (404 `"O artigo não existe."`) |
| `quantidadeAdquirida` | número inteiro | sim | `"A quantidade adquirida é obrigatória."`; `"A quantidade adquirida tem de ser maior que zero."` |
| `precoUnitario` | número | sim | `"O preço unitário é obrigatório."`; `"O preço unitário não pode ser negativo."`; `"O preço unitário pode ter no máximo 8 algarismos inteiros e 2 casas decimais."` |
| `observacoes` | string | não | Sem limite de tamanho. Um texto só com espaços fica `null`. |

**Códigos:** 201 (com `Location`), 400, 404 (`"A compra não existe."` ou `"O artigo não existe."`), 409 `"Não é possível adicionar itens a uma compra fechada."`

```json
POST /api/compras/5/itens/nao-previstos
{
  "idArtigo": 3,
  "quantidadeAdquirida": 2,
  "precoUnitario": 1.25,
  "observacoes": "Estava em promoção"
}
```

```json
201 Created
Location: http://IP:8080/api/compras/5/itens/4
{
  "id": 4,
  "tipo": "NAO_PREVISTO",
  "artigo": { "id": 3, "nome": "Azeite", "tipo": { "id": 1, "nome": "Mercearia" } },
  "quantidadePrevista": null,
  "quantidadeAdquirida": 2,
  "precoUnitario": 1.25,
  "observacoes": "Estava em promoção"
}
```

### `PUT /api/compras/{idCompra}/itens/{idItem}`

Altera um item de uma compra aberta. Há um só endpoint para os dois tipos; o servidor usa o tipo com que o item foi criado.

**Pedido**

| Campo | Tipo | Previsto | Não previsto |
|---|---|---|---|
| `idArtigo` | número inteiro | obrigatório | obrigatório |
| `quantidadePrevista` | número inteiro | obrigatório, maior que zero | não pode vir |
| `quantidadeAdquirida` | número inteiro | opcional, maior que zero | obrigatório, maior que zero |
| `precoUnitario` | número | opcional, não negativo, 2 casas | obrigatório, não negativo, 2 casas |
| `observacoes` | string | não pode vir | opcional |

As mensagens de validação são as mesmas da criação, incluindo `"O artigo é obrigatório."` e `"A quantidade adquirida e o preço unitário têm de ser indicados em conjunto."` Estas mensagens dependem do tipo do item:

| Situação | Mensagem (400) |
|---|---|
| Previsto sem `quantidadePrevista` | `"A quantidade prevista é obrigatória."` |
| Previsto com `observacoes` | `"Um item previsto não tem observações."` |
| Não previsto com `quantidadePrevista` | `"Um item não previsto não tem quantidade prevista."` |
| Não previsto sem `quantidadeAdquirida` e `precoUnitario` | `"Um item não previsto tem de ter a quantidade adquirida e o preço unitário."` |

O `PUT` substitui o item todo. Num previsto já adquirido, omitir `quantidadeAdquirida` e `precoUnitario` volta a pô-lo como não adquirido.

**Códigos:** 200, 400, 404 (`"A compra não existe."`, `"O item de compra não existe."` ou `"O artigo não existe."`), 409 `"Não é possível alterar itens de uma compra fechada."`

Exemplo: marcar um item previsto como adquirido.

```json
PUT /api/compras/5/itens/2
{
  "idArtigo": 1,
  "quantidadePrevista": 2,
  "quantidadeAdquirida": 2,
  "precoUnitario": 0.99
}
```

```json
200 OK
{
  "id": 2,
  "tipo": "PREVISTO",
  "artigo": { "id": 1, "nome": "Arroz", "tipo": { "id": 1, "nome": "Mercearia" } },
  "quantidadePrevista": 2,
  "quantidadeAdquirida": 2,
  "precoUnitario": 0.99,
  "observacoes": null
}
```

### `DELETE /api/compras/{idCompra}/itens/{idItem}`

Apaga um item de uma compra aberta.

**Códigos:** 204, 400, 404 (`"A compra não existe."` ou `"O item de compra não existe."`), 409 `"Não é possível apagar itens de uma compra fechada."`

## 10. Regras de negócio

| Regra | O que acontece |
|---|---|
| Uma compra fechada é imutável | `PUT` e `DELETE` da compra, fechar outra vez, e adicionar, alterar ou apagar itens dão 409. Ler a compra e os itens continua a funcionar. |
| O tipo do item é fixo | Um item previsto não passa a não previsto nem o contrário. Um `PUT` com campos do outro tipo dá 400. Para mudar, apaga-se o item e cria-se outro. |
| (`ano`, `mes`) único nos orçamentos | Só pode haver um orçamento por mês. Criar ou alterar para um mês que já tem orçamento dá 409. |
| Nome do tipo de artigo único | Sem distinguir maiúsculas: `"Bebidas"` e `"BEBIDAS"` são o mesmo nome (409). |
| Nome do artigo único por tipo | Sem distinguir maiúsculas, dentro do mesmo tipo. O mesmo nome pode existir em tipos diferentes. |
| Username único | Sem distinguir maiúsculas, também no login. |
| Apagar com registos associados | Dá 409 para um tipo de artigo com artigos, um artigo usado em itens de compra, e um utilizador que criou, alterou ou fechou compras ou orçamentos. O utilizador autenticado não se pode apagar a si próprio (409). |
| Apagar uma compra aberta | Apaga também os seus itens. |
| Auditoria | Quem criou, alterou e fechou, e quando, é preenchido pelo servidor com o utilizador do token. Os pedidos não têm campos para isto. |

Filtros disponíveis nas listagens:

| Endpoint | Filtro | Exemplo |
|---|---|---|
| `GET /api/artigos` | `idTipoArtigo` (inteiro) | `/api/artigos?idTipoArtigo=1` |
| `GET /api/orcamentos` | `ano` (inteiro) | `/api/orcamentos?ano=2026` |
| `GET /api/compras` | `fechada` (`true` ou `false`) | `/api/compras?fechada=false` |

Sem filtro, a listagem devolve tudo. Um filtro sem resultados devolve `[]` com 200. Um filtro com valor do tipo errado dá 400.

## 11. Fluxo de exemplo

Uma compra do início ao fim. Os ids são de exemplo; em cada passo usa-se o `id` devolvido no passo anterior.

**1. Login**

```json
POST /api/auth/login
{ "username": "ana.silva", "password": "palavra-passe-exemplo" }

200 OK
{ "token": "TOKEN", "tipo": "Bearer", "expiraEm": "2026-11-12T21:00:00Z",
  "utilizador": { "id": 1, "nome": "Ana Silva", "username": "ana.silva" } }
```

A partir daqui, todos os pedidos levam `Authorization: Bearer TOKEN`.

**2. Criar a compra**

```json
POST /api/compras
{ "nome": "Compras da semana" }

201 Created
{ "id": 5, "nome": "Compras da semana", "fechada": false, ... }
```

**3. Adicionar um item previsto**

O `idArtigo` vem de `GET /api/artigos`.

```json
POST /api/compras/5/itens/previstos
{ "idArtigo": 1, "quantidadePrevista": 2 }

201 Created
{ "id": 2, "tipo": "PREVISTO", "quantidadePrevista": 2, "quantidadeAdquirida": null, ... }
```

No supermercado, marca-se como adquirido com um `PUT`:

```json
PUT /api/compras/5/itens/2
{ "idArtigo": 1, "quantidadePrevista": 2, "quantidadeAdquirida": 2, "precoUnitario": 0.99 }

200 OK
```

**4. Adicionar um item não previsto**

```json
POST /api/compras/5/itens/nao-previstos
{ "idArtigo": 3, "quantidadeAdquirida": 1, "precoUnitario": 4.49, "observacoes": "Estava em promoção" }

201 Created
{ "id": 4, "tipo": "NAO_PREVISTO", "quantidadeAdquirida": 1, "precoUnitario": 4.49, ... }
```

**5. Fechar a compra**

```json
POST /api/compras/5/fechar

200 OK
{ "id": 5, "fechada": true, "fechadaPor": { "id": 1, ... }, "dataFechada": "2026-11-12T18:05:41.230001Z", ... }
```

A partir daqui, qualquer tentativa de alterar a compra ou os itens dá 409.
