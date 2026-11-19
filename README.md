# iShopping

Protótipo de aplicação de gestão de compras domésticas. Permite manter uma lista de artigos organizados por tipo, definir um orçamento mensal, planear compras com itens previstos, registar o que se compra (incluindo o que não estava previsto) e fechar a compra no fim.

Desenvolvido no âmbito da unidade curricular de Laboratório de Desenvolvimento II, CTeSP em Desenvolvimento para Dispositivos Móveis, ISTEC, 2026/2027.

## Grupo

Turma L, Grupo 1

- Simão Monteiro (2025417)
- Iryna Ungurian (2025475)
- Gonçalo Dias (2025185)

## Índice

1. [Funcionalidades](#funcionalidades)
2. [Tecnologias](#tecnologias)
3. [Estrutura do repositório](#estrutura-do-repositório)
4. [Pré-requisitos](#pré-requisitos)
5. [Configuração (.env)](#configuração-env)
6. [Base de dados](#base-de-dados)
7. [Backend](#backend)
8. [App móvel](#app-móvel)
9. [Resolução de problemas](#resolução-de-problemas)
10. [Fluxo de trabalho](#fluxo-de-trabalho)

## Funcionalidades

Previstas para o projeto:

- Login de utilizadores
- Gestão de utilizadores, tipos de artigo, artigos e orçamentos
- Planeamento de compras e modo Compra
- Exportação de compras fechadas para CSV
- Estatísticas e sugestões de orçamento e de lista de compras

Estado atual:

- **Backend:** API com login, utilizadores, tipos de artigo, artigos, orçamentos, compras (com fecho) e itens de compra previstos e não previstos. A documentação dos endpoints está em [`docs/api.md`](docs/api.md).
- **App móvel:** ecrã de login ligado à API, ecrã para configurar o endereço do servidor e ecrã inicial.

## Tecnologias

| Parte | Tecnologia |
|---|---|
| Backend | Java 25, Spring Boot 4.1.2, Maven (através do `./mvnw`, não é preciso instalar o Maven) |
| Autenticação | JWT, com palavras-passe guardadas com BCrypt |
| Base de dados | PostgreSQL 16, a correr em Docker com Docker Compose |
| App móvel | React Native com Expo (SDK 58), React Navigation e Axios |

## Estrutura do repositório

```
backend/             API em Spring Boot (pacote pt.istec.ishopping)
db/schema.sql        esquema da base de dados (8 tabelas)
db/seed.sql          dados de exemplo (tipos de artigo e artigos)
docs/api.md          documentação dos endpoints da API
mobile/              app móvel em React Native com Expo
mobile/.env.example  modelo do .env.local da app (endereço da API por omissão)
docker-compose.yml   contentor do PostgreSQL
.env.example         modelo do ficheiro de configuração .env
```

## Pré-requisitos

| Ferramenta | Versão | Para quê |
|---|---|---|
| [Java JDK](https://adoptium.net/) (Temurin) | 25 | Backend. É a versão definida no `backend/pom.xml`. |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | atual, com Docker Compose | Base de dados. Tem de estar aberto sempre que se usa o projeto. |
| [Node.js](https://nodejs.org/) com npm | o projeto não fixa versão; a equipa usa a 26.3.0 | App móvel |
| [Expo Go](https://expo.dev/go) no telemóvel | compatível com o Expo SDK 58 (a versão atual da loja) | Abrir a app no telemóvel |
| Git | qualquer versão recente | Clonar o repositório |

Os comandos deste ficheiro foram escritos para Windows. Os blocos marcados como Git Bash funcionam também em macOS e Linux.

## Configuração (.env)

Toda a configuração local fica num ficheiro `.env` na raiz do repositório. É lido pelo Docker Compose e pelo backend.

Na raiz do repositório, copia o modelo:

```bash
cp .env.example .env
```

(Em PowerShell: `Copy-Item .env.example .env`; em cmd: `copy .env.example .env`.)

Depois abre o `.env` e muda os valores. O `.env` tem palavras-passe e o segredo dos tokens, por isso **nunca vai para o Git**: está no `.gitignore` e não se deve forçar a sua inclusão. O `.env.example` só tem valores de exemplo.

| Variável | Exemplo | Para que serve |
|---|---|---|
| `POSTGRES_DB` | `ishopping` | Nome da base de dados criada no contentor |
| `POSTGRES_USER` | `ishopping` | Utilizador do PostgreSQL |
| `POSTGRES_PASSWORD` | `mudar-esta-palavra-passe` | Palavra-passe do PostgreSQL. Escolhe uma tua. |
| `DB_PORT` | `5432` | Porta do PostgreSQL no teu PC. Muda-a se a 5432 já estiver ocupada (ver [Resolução de problemas](#resolução-de-problemas)). É usada pelo Docker Compose e pelo backend. |
| `JWT_SECRET` | `exemplo-falso-substituir-por-segredo-aleatorio-longo` | Segredo usado para assinar os tokens de login. **Mínimo de 32 caracteres**; com menos, ou vazio, o backend não arranca. Usa um texto aleatório e não o partilhes. |
| `JWT_EXPIRACAO_HORAS` | `8` | Horas de validade de cada token (mínimo 1). Se não estiver definida, são 8. |
| `ADMIN_USERNAME` | `admin.exemplo` | Username do primeiro utilizador, criado automaticamente pelo backend |
| `ADMIN_NOME` | `Administrador de Exemplo` | Nome desse utilizador. **Escreve-o sem acentos**, porque podem aparecer mal na aplicação. |
| `ADMIN_PASSWORD` | `mudar-esta-palavra-passe` | Palavra-passe desse utilizador, para entrar na app |

## Base de dados

Com o Docker Desktop aberto, na raiz do repositório:

```bash
docker compose up -d
```

Na primeira vez, isto descarrega a imagem `postgres:16`, cria o contentor `ishopping-db` e corre automaticamente:

- `db/schema.sql`, que cria as 8 tabelas: `utilizador`, `tipo_artigo`, `artigo`, `orcamento`, `compra`, `item_compra`, `item_compra_previsto` e `item_compra_nao_previsto`;
- `db/seed.sql`, que cria dados de exemplo: 6 tipos de artigo (Mercearia, Laticínios, Frutas e Legumes, Carne e Peixe, Limpeza, Higiene) e 15 artigos.

O seed não cria utilizadores. O primeiro utilizador é criado pelo backend (ver [Backend](#backend)).

Os dados ficam guardados num volume do Docker e não se perdem ao desligar o PC. O contentor volta a arrancar sozinho quando o Docker Desktop abre.

Para confirmar que está a correr:

```bash
docker compose ps
```

**Os ficheiros `db/*.sql` só são aplicados quando a base de dados é criada pela primeira vez.** Mudar o `.env` (por exemplo, `POSTGRES_PASSWORD`) ou os ficheiros `db/` depois disso não tem efeito numa base de dados já existente.

### Repor a base de dados do zero

> ⚠️ **Isto apaga todos os dados**: utilizadores, orçamentos, compras e tudo o que tenha sido criado. Não há forma de os recuperar.

```bash
docker compose down -v
docker compose up -d
```

O `-v` apaga o volume com os dados. A seguir, o `up -d` cria tudo de novo a partir de `db/schema.sql` e `db/seed.sql` e com os valores atuais do `.env`. No arranque seguinte, o backend volta a criar o utilizador administrador.

## Backend

O backend usa o Java 25. Se o comando `java -version` não mostrar a versão 25, define a variável `JAVA_HOME` com a pasta onde o JDK 25 está instalado antes de arrancar. Nos exemplos abaixo, o caminho é o da instalação por omissão do Temurin; confirma o nome da pasta em `C:\Program Files\Eclipse Adoptium\`, porque o número da versão pode ser diferente.

A base de dados tem de estar a correr (`docker compose up -d`).

**Git Bash**

```bash
cd backend
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run
```

**PowerShell**

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw spring-boot:run
```

**cmd**

```bat
cd backend
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
mvnw spring-boot:run
```

Estas variáveis só valem para a janela do terminal onde são definidas. Na primeira vez, o `mvnw` descarrega o Maven e as dependências, o que pode demorar alguns minutos.

A API fica em `http://localhost:8080/api`. Para parar o backend, carrega em `Ctrl+C`.

### Utilizador administrador

No primeiro arranque, se a tabela de utilizadores estiver vazia, o backend cria um utilizador com os valores `ADMIN_USERNAME`, `ADMIN_NOME` e `ADMIN_PASSWORD` do `.env`. Aparece no log a linha `Primeiro utilizador criado com o username ...`.

- Se faltar alguma dessas variáveis, não é criado nenhum utilizador e aparece um aviso no log.
- Depois de existir pelo menos um utilizador, estas variáveis deixam de ser usadas. Mudá-las no `.env` não altera o utilizador que já existe. Para voltar a criá-lo, é preciso repor a base de dados.

Com este utilizador, entra-se na app e podem criar-se os restantes utilizadores.

### Confirmar que está a funcionar

Abre no browser, ou com `curl`:

```
http://localhost:8080/actuator/health
```

A resposta deve ser `{"status":"UP"}`.

### Testes

Na pasta `backend/`, com o `JAVA_HOME` definido como acima:

```bash
./mvnw clean test
```

Em PowerShell é `.\mvnw clean test` e em cmd é `mvnw clean test`. Os testes não precisam da base de dados nem do Docker.

## App móvel

O telemóvel com o Expo Go e o PC onde corre o backend têm de estar **na mesma rede Wi-Fi**.

### 1. Configurar o endereço da API

A app precisa do endereço do PC onde corre o backend. O telemóvel não consegue usar `localhost`, por isso começa por descobrir o IP do PC na rede atual:

1. Abre um terminal e corre `ipconfig`.
2. No adaptador de rede que estás a usar (normalmente Wi-Fi), copia o **Endereço IPv4**, por exemplo `192.168.1.50`.

O endereço a usar é então `http://192.168.1.50:8080`, **sem `/api` no fim**, porque a app acrescenta-o sozinha. O IP muda quando mudas de rede (casa, escola).

Há duas formas de indicar este endereço à app. Podes usar só uma ou as duas.

**a) Endereço por omissão, no ficheiro `mobile/.env.local`**

Na pasta `mobile/`, copia o modelo:

```bash
cp .env.example .env.local
```

(Em PowerShell: `Copy-Item .env.example .env.local`; em cmd: `copy .env.example .env.local`.)

Depois põe o teu IP na variável `EXPO_PUBLIC_API_URL`:

```
EXPO_PUBLIC_API_URL=http://192.168.1.50:8080
```

O `.env.local` não vai para o Git (está no `mobile/.gitignore`), por isso cada pessoa tem o seu. Se mudares este ficheiro com o `npx expo start` a correr, para-o e arranca-o de novo. Se o ficheiro não existir, o endereço por omissão é `http://10.0.2.2:8080`, que só funciona no emulador de Android (aponta para o PC onde o emulador corre).

**b) Ecrã "Servidor", dentro da app**

No fundo do ecrã de login aparece `Servidor:` com o endereço atual e a opção **Alterar**, que abre o ecrã "Servidor". Lá:

- escreve o endereço no campo **Endereço do servidor**. Também aceita só `192.168.1.50:8080`: a app acrescenta o `http://` e retira um `/` ou `/api` final;
- **Testar ligação** confirma se o backend responde nesse endereço;
- **Guardar** guarda o endereço no telemóvel e volta ao login;
- **Repor endereço por defeito** apaga o endereço guardado e volta ao da alínea a).

O ecrã "Servidor" só está disponível no login, antes de entrar.

**Que endereço é usado:** sempre que a app arranca, usa o endereço guardado no ecrã "Servidor", se houver um. Se não houver, usa o `EXPO_PUBLIC_API_URL` do `.env.local`, ou o `http://10.0.2.2:8080` quando a variável não está definida. Por isso, depois de guardares um endereço no ecrã "Servidor", mudar o `.env.local` deixa de ter efeito nesse telemóvel até carregares em **Repor endereço por defeito**.

### 2. Instalar e arrancar

Na primeira vez, e sempre que o `package.json` mudar:

```bash
cd mobile
npm install
```

Para arrancar:

```bash
npx expo start
```

Aparece um código QR no terminal. No Android, lê-o com a app Expo Go; no iPhone, lê-o com a câmara. A app abre no telemóvel.

### 3. Entrar

No ecrã de login, usa o `ADMIN_USERNAME` e a `ADMIN_PASSWORD` do teu `.env`. O username não distingue maiúsculas de minúsculas.

## Resolução de problemas

### A porta 5432 já está ocupada

O `docker compose up -d` falha com uma mensagem a dizer que a porta 5432 não está disponível ou já está atribuída. Normalmente é porque há outro PostgreSQL instalado no PC.

Escolhe outra porta no `.env`, por exemplo:

```
DB_PORT=5433
```

Depois volta a correr `docker compose up -d`. Não é preciso mudar mais nada: o backend lê a mesma variável.

### "password authentication failed for user"

O backend não consegue entrar na base de dados. As causas mais comuns:

- **O `POSTGRES_PASSWORD` mudou depois de a base de dados ter sido criada.** A palavra-passe fica definida na criação e o `.env` deixa de ter efeito. Repõe o valor antigo no `.env` ou, se puderes perder os dados, repõe a base de dados com `docker compose down -v` e `docker compose up -d` (ver [aviso](#repor-a-base-de-dados-do-zero)).
- **O backend está a ligar-se a outro PostgreSQL** instalado no PC na mesma porta. Usa outra porta com `DB_PORT` (ver acima).
- **O contentor não está a correr.** Confirma com `docker compose ps` e que o Docker Desktop está aberto.

### A app mostra "Não foi possível ligar ao servidor. Verifica o endereço em "Servidor"."

A app não chegou ao backend. Verifica, por esta ordem:

1. **O backend está a correr.** No PC, `http://localhost:8080/actuator/health` deve responder `{"status":"UP"}`.
2. **O endereço está certo.** No login, vê o endereço em `Servidor:`. Tem de ter o IPv4 atual do PC (`ipconfig`) e a porta 8080, por exemplo `http://192.168.1.50:8080`. Se estiver errado, corrige-o em **Alterar** e usa **Testar ligação** (ver [Configurar o endereço da API](#1-configurar-o-endereço-da-api)).
3. **Estão na mesma rede.** O telemóvel tem de estar no mesmo Wi-Fi que o PC, e não nos dados móveis. Algumas redes, como as de escolas, impedem os dispositivos de comunicarem entre si; nesse caso, liga o PC ao hotspot do telemóvel.
4. **A firewall do Windows não bloqueia a porta 8080.** Para testar, abre no browser do telemóvel `http://IP-DO-PC:8080/actuator/health`. Se não responder, permite a porta 8080. Na primeira vez que o Java pede acesso à rede, o Windows mostra um aviso; aceita para redes privadas. Também podes criar a regra numa PowerShell aberta como administrador:

   ```powershell
   New-NetFirewallRule -DisplayName "iShopping API 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
   ```

   Se a rede Wi-Fi estiver definida no Windows como pública, as ligações vindas de outros dispositivos podem continuar bloqueadas. Muda-a para privada nas definições de rede.

### O backend não arranca: "A variável JWT_SECRET..."

O `JWT_SECRET` falta no `.env` ou tem menos de 32 caracteres. Corrige-o e volta a arrancar.

## Fluxo de trabalho

O projeto segue Scrum, com 4 sprints, acompanhado no Jira (espaço iShopping LDII G1).

- **`main`:** versão estável, apenas entregas de fim de sprint.
- **`develop`:** integração do trabalho em curso.
- **`feature/ILG-<n>-<descricao-curta>`:** uma branch por tarefa do Jira, sempre criada a partir de `develop`.
- **Commits:** Conventional Commits em português, minúsculas, verbo no infinitivo e sem ponto final, por exemplo `feat(ILG-15): adicionar crud de orcamentos`. Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- **Pull Requests:** vão para `develop` e precisam da revisão de um colega antes do merge.
- **Proibições:** nunca se faz commit diretamente em `main` nem em `develop`, e nunca se faz commit do `.env`.

A documentação dos endpoints da API, com os campos, os códigos de resposta e os exemplos, está em [`docs/api.md`](docs/api.md).
