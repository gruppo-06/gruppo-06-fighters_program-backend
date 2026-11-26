# Fighters Program — Backend

Backend del gioco di carte collezionabili. È un **monolite modulare su build Maven multi-modulo**: un `core` in Java puro (il motore di gioco, senza Spring) e i moduli di dominio scritti con Spring Boot. C'è un solo artefatto avviabile, `application`.

Stack: Spring Boot 4, Java 21, Postgres 17, Redis 7, Flyway, Maven.

---

## 1. Perché multi-modulo

Il brief richiede che il **motore di gioco sia Java puro**, senza Spring. In un singolo progetto la regola si può solo controllare con un test; in una build multi-modulo il confine è **imposto dal compilatore**: se `core` non dichiara Spring nel `pom.xml`, nessuno può importarlo per sbaglio.

Quindi:

- `core` = motore di gioco, zero dipendenze di framework (nemmeno nei test).
- ogni modulo di dominio = libreria Spring (JPA, webmvc) che può dipendere da `core` ma non viceversa.
- `application` = unico modulo con il `main`, la configurazione e le migrazioni; dipende da tutti gli altri.

Restano un monolite (un solo processo, un solo database), quindi niente microservizi e niente deploy indipendenti: i moduli servono a tenere i confini netti, non a separare il runtime.

## 2. Moduli e dipendenze

| Modulo | Artifact | Responsabilità |
|---|---|---|
| `core` | `fighters-program-core` | Motore di gioco in Java puro (mazzi, round, fazioni, seed) |
| `common` | `fighters-program-common` | Formato unico degli errori e paginazione. Non dipende dagli altri moduli |
| `catalog` | `fighters-program-catalog` | Carte, fazioni, classi, rarità (di riferimento) |
| `collection` | `fighters-program-collection` | Carte possedute e mazzi, validazione dei mazzi |
| `identity` | `fighters-program-identity` | Utenti, registrazione, login, JWT |
| `economy` | `fighters-program-economy` | Crediti e pacchetti |
| `trading` | `fighters-program-trading` | Scambi tra utenti |
| `cosmetics` | `fighters-program-cosmetics` | Catalogo e proprietà delle skin cosmetiche |
| `matches` | `fighters-program-matches` | Sfide, simulazione, replay, storico |
| `application` | `fighters-program-application` | Main, configurazione, migrazioni. Unico avviabile |

Dipendenze tra moduli (`X → Y` significa che `X` dipende da `Y`):

```
core         →  (nessuna dipendenza dell'app)
common       →  (nessuna dipendenza dell'app)
catalog      →  common
collection   →  common
identity     →  common
economy      →  common
trading      →  common
cosmetics    →  common, catalog, collection, identity, economy
matches      →  common, core
application  →  tutti gli altri moduli
```

Regole di dipendenza:

1. `core` non dipende da nulla dell'app: solo `java..`.
2. `common` non dipende da nessun modulo di dominio né dal core.
3. Un modulo di dominio dipende da `common` e, se gli serve il motore, da `core`. Le dipendenze tra moduli di dominio sono esplicite nella tabella sopra e usano solo le API pubbliche nella radice del package.
4. `application` dipende da tutti e li mette insieme.

## 3. Struttura interna di un modulo

Ogni modulo di dominio ha la stessa forma (guarda `catalog`, che è l'esempio completo):

```
it.fightersprogram.<modulo>
├── <Modulo>Service.java        logica del modulo; è la sua API PUBBLICA
├── <Qualcosa>Response.java     record restituiti dal servizio (DTO pubblici)
├── domain/                     entità JPA, repository, specification  → PRIVATO
└── web/                        controller REST                        → PRIVATO
```

- Radice del package (`it.fightersprogram.<modulo>`) = **API pubblica** del modulo: solo servizi e record.
- `domain/` e `web/` = **interni**. Nessun altro modulo li può usare.
- Il servizio restituisce sempre **record**, mai entità JPA: le entità non escono dal modulo.
- Il controller è sottile: legge i parametri, chiama il servizio, restituisce il risultato. Non conosce i repository.

Per parlare con un altro modulo si usa il suo **servizio pubblico** o un evento, mai i suoi interni.

## 4. Regole imposte da build e test

Il confine del core è imposto dal `pom.xml` di `core` (nessuna dipendenza Spring/JPA/Jackson/Lombok). Tutto il resto è verificato dai test in `application` (`ArchitectureTest`, ArchUnit) e in `core` (`EngineIsolationTest`):

| Regola | Dove |
|---|---|
| `core` dipende solo da `java..` | `pom.xml` di `core` + `ArchitectureTest.engineIsPureJava` |
| `engine` senza `import` di framework | `EngineIsolationTest` (legge i sorgenti di `engine`) |
| `common` non dipende dai moduli | `ArchitectureTest.commonDoesNotDependOnModules` |
| niente cicli tra moduli | `ArchitectureTest.modulesAreFreeOfCycles` |
| i controller non usano i repository | `ArchitectureTest.controllersDoNotUseRepositories` |
| nessun modulo usa gli interni di un altro | `ArchitectureTest.modulesDoNotUseEachOthersInternals` |

Se `sh ./mvnw verify` fallisce su uno di questi test, hai violato una regola: **correggi il codice, non il test**.

## 5. Il core in Java puro

Il motore di gioco (`core`, package `it.fightersprogram.engine`) prende in ingresso due mazzi e un seed e produce il risultato della partita con il log turno per turno. Vincoli:

- Niente Spring, JPA, Jackson o Lombok, **nemmeno nei test** (i test usano solo JUnit).
- Determinismo: stesso mazzo + stesso seed = stessa partita (il seed è un parametro, mai generato dentro il motore).
- Non conosce il database né la rete: è pura logica di dominio.

`EngineIsolationTest` scansiona i file `.java` di `engine` e fallisce se compare un import di framework. Il `pom.xml` di `core` non dichiara nessuna dipendenza Spring, quindi un import del genere nemmeno compila: il test è una rete di sicurezza in più.

## 6. Flusso di una richiesta (esempio)

`GET /api/cards/EA01`:

1. `application` avvia Spring e fa il component scan da `it.fightersprogram`: registra i bean di tutti i moduli.
2. `catalog/web/CatalogController` riceve la richiesta e chiama `CatalogService.findByCode("EA01")`.
3. `CatalogService` interroga `CardRepository`, carica l'entità `Card` e la converte in `CardResponse` (record). Se non la trova, lancia `NotFoundException`.
4. `GlobalExceptionHandler` (in `common`) trasforma l'eccezione in JSON `{ "code", "message", "details" }`.
5. Il controller restituisce il record: Spring lo serializza in JSON.

Il modulo `catalog` non conosce né il controller di un altro modulo, né le sue entità: comunica solo attraverso la propria API pubblica.

## 7. Persistenza e migrazioni

- **Postgres 17** + **Redis 7** avviati con `docker compose`.
- Schema gestito **solo** da Flyway: `spring.jpa.hibernate.ddl-auto=validate` (Hibernate non crea né modifica tabelle).
- Le migrazioni stanno in `application/src/main/resources/db/migration`, nome `V<numero>__<descrizione>.sql`.
- Nomi di tabelle e colonne in inglese, `snake_case` e keyword SQL maiuscole.
- **Non modificare mai una migrazione già pushata**: se serve una correzione, si crea una nuova versione.

## 8. Sicurezza

- Configurazione in `application`, package `it.fightersprogram.config` (`SecurityConfig`): è il composition root.
- Sessione stateless, JWT come resource server OAuth2.
- Pubblici: `GET /api/cards/**`, `GET /api/skins/**`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`), `/actuator/health`. Tutto il resto richiede autenticazione.
- Errori di sicurezza nello stesso formato JSON di `common`.
- CORS su `/api/**` solo per le origini in `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`), con gli header `Authorization`, `Content-Type` e `Idempotency-Key`.

## 9. Configurazione e avvio locale

```bash
cd fighters-program
docker compose up -d
export JWT_SECRET="$(openssl rand -base64 48)"   # obbligatorio, almeno 32 caratteri
sh ./mvnw -pl application -am spring-boot:run
```

Swagger: http://localhost:8080/swagger-ui.html

La configurazione (`application/src/main/resources/application.properties`) legge da variabili d'ambiente con default per il locale:

| Variabile | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/fighters` |
| `SPRING_DATASOURCE_USERNAME` | `fighters` |
| `SPRING_DATASOURCE_PASSWORD` | `fighters` |
| `SPRING_DATA_REDIS_HOST` | `localhost` |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `CATALOG_IMPORT_ENABLED` | `true` (importa all'avvio i JSON del catalogo) |
| `CATALOG_IMPORT_LOCATION` | `classpath*:catalog/*.json` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (dev server di Vite); più origini separate da virgola |
| `JWT_SECRET` | nessuno: **obbligatorio**, almeno 32 caratteri, mai nel codice né nel repo |
| `ADMIN_EMAIL` | nessuno (opzionale: se valorizzata insieme a `ADMIN_PASSWORD`, crea all'avvio l'utente amministratore iniziale se assente) |
| `ADMIN_PASSWORD` | nessuno (opzionale: almeno 12 caratteri se `ADMIN_EMAIL` è impostata; mai nel codice né nel repo) |
| `ADMIN_NICKNAME` | `admin` |

I test usano una chiave di prova in `application/src/test/resources/config/application.properties`.

### Formato degli errori

Ogni errore dell'API ha la forma `{ "code": "...", "message": "...", "details": [] }`.

| HTTP | `code` | Quando |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Campi non validi; `details` elenca `campo: motivo` |
| 400 | `MALFORMED_REQUEST` | Corpo della richiesta non leggibile come JSON |
| 400 | `BAD_REQUEST` | Richiesta valida nella forma ma non sensata (es. sfidare sé stessi) |
| 401 | `UNAUTHORIZED` | Token assente, scaduto o non valido; credenziali errate |
| 403 | `FORBIDDEN` | Ruolo insufficiente (es. `/api/admin/**` senza ADMIN) |
| 404 | `NOT_FOUND` | Risorsa inesistente o non visibile all'utente |
| 409 | `CONFLICT` | Duplicato o stato incompatibile (email già usata, sfida già chiusa) |
| 422 | `INVALID_DECK` | Mazzo che viola le regole; `details` elenca le violazioni |

### Violazioni del mazzo

`POST /api/me/decks/validate` risponde sempre 200 con `{ "valid": true|false, "violations": [{ "code", "message" }] }`;
creazione e modifica del mazzo rispondono 422 `INVALID_DECK` con le stesse violazioni in `details`.
I limiti sono in `game.deck.*` (`application.properties`), i messaggi in
`collection/src/main/resources/it/fightersprogram/collection/deck-violations.properties`.

| `code` | Quando |
|---|---|
| `WRONG_SIZE` | Il totale delle copie è diverso da `game.deck.size` |
| `TOO_MANY_COPIES` | Una carta supera `game.deck.max-copies` |
| `TOO_MANY_EPIC` | Le Epiche superano `game.deck.max-epic` |
| `TOO_MANY_LEGENDARY` | Le Leggendarie superano `game.deck.max-legendary` |
| `OVER_BUDGET` | La somma dei punti di rarità (`game.deck.rarity-points`) supera `game.deck.power-budget` |
| `NOT_OWNED` | Copie richieste oltre quelle possedute |
| `UNKNOWN_CARD` | `cardId` inesistente nel catalogo |
| `DUPLICATE_CARD` | La stessa carta su più righe |

### Endpoint principali (dettagli su Swagger)

Tutti richiedono `Authorization: Bearer <token>`, tranne registrazione, login e catalogo.

| Metodo e percorso | Cosa fa |
|---|---|
| `POST /api/auth/register` | Registra un giocatore (riceve il set iniziale provvisorio, decisione 003, e 500 crediti di benvenuto) |
| `POST /api/auth/login` | Restituisce `{ accessToken, expiresIn }` |
| `GET /api/me` | Profilo dell'utente corrente |
| `GET /api/cards`, `GET /api/cards/{code}` | Catalogo pubblico (l'elenco mostra solo le carte attive) |
| `GET /api/skins`, `GET /api/skins/{code}` | Catalogo pubblico delle skin attive, filtrabile per tipo e carta |
| `POST /api/skins/{id}/purchase` | Acquista una skin con crediti |
| `GET /api/me/skins` | Skin possedute dall'utente autenticato, con data di acquisizione |
| `PUT /api/me/skins/avatar`, `PUT /api/me/skins/cards/{cardId}` | Seleziona avatar e varianti delle carte possedute |
| `GET /api/players/{nickname}/appearance` | Aspetto cosmetico del giocatore (solo utenti autenticati) |
| `POST /api/admin/cards`, `PUT /api/admin/cards/{code}` | Crea o modifica una carta (solo ADMIN); l'importer non la sovrascrive più |
| `DELETE /api/admin/cards/{code}` | Disattiva una carta (solo ADMIN): esce da catalogo e pacchetti, resta nelle collezioni; `PUT` con `"active": true` la riattiva |
| `GET /api/me/collection`, `GET /api/me/collection/progress` | Carte possedute e avanzamento per fazione |
| `GET /api/me/credits` | Saldo crediti (somma dei movimenti) e ultimi 20 movimenti |
| `GET/POST /api/me/credits/daily` | Stato e ritiro del bonus giornaliero, con serie di giorni consecutivi (giorno di gioco: Europe/Rome) |
| `GET /api/packs`, `GET /api/packs/{id}` | Pacchetti attivi con prezzo e carte per rarità (pubblici) |
| `POST /api/packs/{id}/open` | Apre un pacchetto: addebito e carte in un'unica transazione; header `Idempotency-Key` obbligatorio, saldo insufficiente 402 |
| `POST /api/admin/users/{nickname}/credits` | Accredito o addebito manuale (solo ADMIN, header `Idempotency-Key`) |
| `GET/POST /api/admin/packs`, `PUT/DELETE /api/admin/packs/{id}` | Gestione dei pacchetti (solo ADMIN); `DELETE` li disattiva |
| `POST /api/me/decks/validate` | Verifica un mazzo senza salvarlo |
| `GET/POST /api/me/decks`, `GET/PUT/DELETE /api/me/decks/{id}` | Mazzi dell'utente |
| `POST /api/matches` | Sfida un giocatore: `{ opponentNickname, deckId }` |
| `GET /api/matches/incoming`, `GET /api/matches/outgoing` | Sfide ricevute in attesa e sfide inviate |
| `POST /api/matches/{id}/accept` | Accetta con `{ deckId }`: il server simula e restituisce il replay; il vincitore riceve 50 crediti |
| `POST /api/matches/{id}/decline`, `POST /api/matches/{id}/cancel` | Rifiuta (sfidato) o annulla (sfidante) |
| `GET /api/matches/{id}` | Replay: mazzi e log turno per turno (solo partecipanti) |
| `GET /api/matches/history` | Storico paginato con esito `WIN`, `LOSS`, `DRAW` |
| `POST /api/trades` | Propone uno scambio a un altro giocatore |
| `GET /api/trades/incoming`, `GET /api/trades/outgoing` | Scambi ricevuti PENDING e scambi inviati in ogni stato |
| `GET /api/trades/{id}` | Dettaglio dello scambio (solo partecipanti) |
| `POST /api/trades/{id}/accept` | Accetta uno scambio e trasferisce le carte |
| `POST /api/trades/{id}/decline`, `POST /api/trades/{id}/cancel` | Rifiuta uno scambio ricevuto o annulla uno proposto |

Il frontend usa `GET /api/players/{nickname}/appearance` per aggiungere avatar e varianti grafiche a profilo, collezione
e replay. Questi endpoint restituiscono i dati di gioco senza skin: il frontend unisce l'aspetto cosmetico alla risposta
principale.

## 10. Test

- **Unità del core** (`core`): `sh ./mvnw -pl core test`. Veloci, senza Spring.
- **Confini architetturali** (`application`): `ArchitectureTest` (ArchUnit) e `EngineIsolationTest`.
- **Integrazione** (`application`): `FightersProgramApplicationTests` è un `@SpringBootTest` che carica il contesto e richiede Postgres e Redis attivi (`docker compose up -d`).

```bash
cd fighters-program
sh ./mvnw verify                                  # tutti i moduli e tutti i test
sh ./mvnw -pl core test                           # solo il motore in Java puro
sh ./mvnw -pl application -am test                # app + moduli da cui dipende
sh ./mvnw verify -Dtest='!FightersProgramApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false
```

## 11. Aggiungere una funzionalità

1. Crea il branch da `develop`: `feature/<descrizione-con-trattini>`.
2. Se serve una tabella: nuova migrazione in `application/src/main/resources/db/migration`.
3. Entità e repository in `<modulo>/domain/` (guarda `catalog/domain/Card.java`).
4. Record di richiesta/risposta (guarda `CardResponse`).
5. Servizio con la logica, `@Transactional` (guarda `CatalogService`).
6. Controller in `<modulo>/web/`, sottile (guarda `CatalogController`).
7. Errori: lancia `NotFoundException` o `ConflictException` da `common`.
8. Descrivi gli endpoint con `@Tag` e `@Operation`.
9. `sh ./mvnw verify` deve passare; prova l'endpoint da Swagger.

## 12. Aggiungere un nuovo modulo

1. Crea la cartella `fighters-program/<modulo>/` con un `pom.xml` che ha come `parent` `fighters-program` e dichiara le dipendenze necessarie (`fighters-program-common`, ed eventualmente `fighters-program-core`).
2. Aggiungi `<module>nome</module>` in `fighters-program/pom.xml` e, se lo espone ad altri, la dipendenza gestita in `<dependencyManagement>`.
3. Aggiungi la dipendenza del modulo in `application/pom.xml`.
4. Crea il package `it.fightersprogram.<modulo>` con un `package-info.java` che spiega cosa fa il modulo.
5. Segui la struttura del punto 3.

## 13. Prima di aprire la PR

- `sh ./mvnw verify` deve passare, con `docker compose up -d` attivo.
- Commit piccoli, in inglese: `feat: ...`, `fix: ...`, `test: ...`, `docs: ...`.
- PR verso `develop`, titolo nel formato Conventional Commits.
- Nel codice niente segreti, niente `System.out.println`.

## 14. Convenzioni di codice

- Codice, nomi di classi, variabili, messaggi di log/eccezioni e commit in inglese.
- Nomi chiari al posto dei commenti.
- Non aggiungere astrazioni che non servono (niente interfacce con una sola implementazione).
- Le decisioni architetturali importanti si registrano in `docs/decisions.md`.
