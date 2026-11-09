# Fighters Program — Backend

Spring Boot 4, Java 21, Postgres 17, Redis 7. Monolite modulare: un solo progetto, diviso in moduli con confini controllati dai test.

## Avvio in locale

```bash
cd fighters-program
docker compose up -d
sh ./mvnw spring-boot:run
```

Swagger: http://localhost:8080/swagger-ui.html

## Moduli

| Modulo | Cosa contiene |
|---|---|
| `engine` | Motore di gioco in Java puro (niente Spring) |
| `catalog` | Carte, fazioni, classi, rarità |
| `collection` | Carte possedute e mazzi, validazione dei mazzi |
| `identity` | Utenti, registrazione, login, JWT |
| `economy` | Crediti e pacchetti |
| `trading` | Scambi tra utenti |
| `matches` | Sfide, simulazione, replay, storico |
| `common` | Errori, paginazione, sicurezza di base. Non dipende dai moduli |

## Come è fatto un modulo (copia `catalog`)

Il modulo `catalog` è l'esempio completo. Un modulo nuovo deve avere la stessa forma:

```
it.fightersprogram.<modulo>
├── <Modulo>Service.java      logica; è l'API pubblica del modulo
├── <Qualcosa>Response.java   record restituiti dal servizio (DTO pubblici)
├── domain/                   entità JPA, repository, specification. Privato del modulo
└── web/                      controller REST. Privato del modulo
```

Regole, controllate dal test `ArchitectureTest`:

1. Il **controller** (`web`) parla solo con il **servizio**, mai con un repository.
2. Il **servizio** restituisce `record`, mai entità JPA.
3. Un modulo **non importa** `web` o `domain` di un altro modulo. Se ti serve qualcosa di un altro modulo, usa il suo servizio pubblico o un evento.
4. `engine` non usa Spring, JPA, Jackson né altri moduli.
5. Niente cicli tra moduli.

Se `./mvnw verify` fallisce su `ArchitectureTest`, hai violato una di queste regole: correggi il codice, non il test.

## Aggiungere una funzionalità, passo per passo

1. Crea il branch da `develop`: `feature/<descrizione-con-trattini>`.
2. Se serve una tabella: nuova migrazione in `src/main/resources/db/migration`, nome `V<numero>__<descrizione>.sql`. Non modificare mai una migrazione già pushata.
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
