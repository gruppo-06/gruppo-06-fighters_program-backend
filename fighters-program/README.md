# Fighters Program — Backend

Spring Boot 4, Java 21, Postgres 17, Redis 7. Build Maven multi-modulo: un `core` in Java puro (niente Spring) e i moduli di dominio Spring Boot, con i confini imposti dal build e controllati dai test.

## Moduli Maven

| Modulo | Artifact | Cosa contiene |
|---|---|---|
| `core` | `fighters-program-core` | Motore di gioco in Java puro. Nessuna dipendenza Spring, JPA, Jackson o Lombok |
| `common` | `fighters-program-common` | Errori (formato unico) e paginazione. Non dipende dagli altri moduli |
| `catalog` | `fighters-program-catalog` | Carte, fazioni, classi, rarità |
| `collection` | `fighters-program-collection` | Carte possedute e mazzi, validazione dei mazzi |
| `identity` | `fighters-program-identity` | Utenti, registrazione, login, JWT |
| `economy` | `fighters-program-economy` | Crediti e pacchetti |
| `trading` | `fighters-program-trading` | Scambi tra utenti |
| `matches` | `fighters-program-matches` | Sfide, simulazione, replay, storico |
| `application` | `fighters-program-application` | Unico modulo avviabile: main, configurazione, migrazioni |

Il package Java dei moduli resta `it.fightersprogram.<modulo>`; il core usa `it.fightersprogram.engine`. La configurazione di sicurezza Spring è in `application` (`it.fightersprogram.config`).

## Avvio in locale

```bash
cd fighters-program
docker compose up -d
sh ./mvnw -pl application -am spring-boot:run
```

Swagger: http://localhost:8080/swagger-ui.html

## Build

```bash
cd fighters-program
sh ./mvnw verify              # compila e testa tutti i moduli
sh ./mvnw -pl core test       # solo il core in Java puro
sh ./mvnw -pl application -am test
```

## Come è fatto un modulo (copia `catalog`)

Il modulo `catalog` è l'esempio completo. Un modulo di dominio deve avere la stessa forma:

```
it.fightersprogram.<modulo>
├── <Modulo>Service.java      logica; è l'API pubblica del modulo
├── <Qualcosa>Response.java   record restituiti dal servizio (DTO pubblici)
├── domain/                   entità JPA, repository, specification. Privato del modulo
└── web/                      controller REST. Privato del modulo
```

Regole, controllate dal build e dai test:

1. Il `core` è un modulo Maven senza dipendenze Spring: il confine è imposto dal `pom.xml`, e `EngineIsolationTest` fallisce se in `engine` compare un import di un framework.
2. Il **controller** (`web`) parla solo con il **servizio**, mai con un repository.
3. Il **servizio** restituisce `record`, mai entità JPA.
4. Un modulo **non importa** `web` o `domain` di un altro modulo. Se ti serve qualcosa di un altro modulo, usa il suo servizio pubblico o un evento.
5. Niente cicli tra moduli.

I controlli ArchUnit (`ArchitectureTest`) stanno in `application`: è l'unico modulo con tutti gli altri sul classpath.

Se `sh ./mvnw verify` fallisce su `ArchitectureTest`, hai violato una di queste regole: correggi il codice, non il test.

## Aggiungere una funzionalità, passo per passo

1. Crea il branch da `develop`: `feature/<descrizione-con-trattini>`.
2. Se serve una tabella: nuova migrazione in `application/src/main/resources/db/migration`, nome `V<numero>__<descrizione>.sql`. Non modificare mai una migrazione già pushata.
3. Entità e repository in `domain/` (guarda `catalog/domain/Card.java`).
4. Record di risposta e richiesta (guarda `CardResponse`).
5. Servizio con la logica, `@Transactional` (guarda `CatalogService`).
6. Controller in `web/`, sottile: legge i parametri, chiama il servizio, restituisce il risultato (guarda `CatalogController`).
7. Errori: lancia `NotFoundException` o `ConflictException`. Il formato JSON è già gestito da `GlobalExceptionHandler`.
8. Descrivi gli endpoint con `@Tag` e `@Operation`.
9. Avvia l'app e prova l'endpoint da Swagger.

## Prima di aprire la PR

- `sh ./mvnw verify` deve passare, con `docker compose up -d` attivo.
- Commit piccoli, in inglese: `feat: ...`, `fix: ...`, `test: ...`, `docs: ...`.
- PR verso `develop`, titolo nel formato Conventional Commits.
- Nel codice niente segreti, niente `System.out.println`.

## Convenzioni di codice

- Codice, nomi di classi, variabili e messaggi di commit in inglese.
- Nomi chiari al posto dei commenti.
- Non aggiungere astrazioni che non servono (niente interfacce con una sola implementazione).
