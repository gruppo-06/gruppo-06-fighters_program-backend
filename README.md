# gruppo-06-nome-progetto-scelto
Laboratorio Integrato – Gioco di carte collezionabili digitale

Il backend sta in [`fighters-program/`](fighters-program/). L'architettura, i moduli e il flusso di lavoro sono spiegati in [`fighters-program/README.md`](fighters-program/README.md).

## Avvio in locale

### Prerequisiti

- Java 21 (`java -version`)
- Docker con il plugin Compose (`docker compose version`)

Maven non serve: il progetto usa il wrapper `mvnw`.

### Comandi

```bash
cd fighters-program
docker compose up -d                              # Postgres 17 e Redis 7
docker compose ps                                 # entrambi i servizi devono risultare "healthy"
sh ./mvnw -pl application -am spring-boot:run     # avvia il backend su http://localhost:8080
```

All'avvio Flyway crea le tabelle; la versione applicata si vede nella tabella `flyway_schema_history`.

- Swagger: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

Per fermare tutto: `Ctrl+C` sul backend, poi `docker compose down`. I dati di Postgres restano nel volume `postgres-data`; `docker compose down -v` li cancella.

### Variabili d'ambiente

I default puntano al compose locale, quindi in locale non serve impostare nulla. La CI usa le stesse variabili.

| Variabile | Default | Uso |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/fighters` | URL JDBC di Postgres |
| `SPRING_DATASOURCE_USERNAME` | `fighters` | Utente del database |
| `SPRING_DATASOURCE_PASSWORD` | `fighters` | Password del database (solo per il locale) |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host di Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Porta di Redis |

### Test

```bash
cd fighters-program
docker compose up -d
sh ./mvnw verify          # tutti i moduli e tutti i test, come in CI
sh ./mvnw -pl core test   # solo il motore di gioco, non serve il database
```

### Problemi frequenti

| Sintomo | Causa e soluzione |
|---|---|
| `Connection to localhost:5432 refused` all'avvio o nei test | Postgres non è attivo: `docker compose up -d` e attendi lo stato `healthy` |
| `port is already allocated` su 5432 o 6379 | Un altro Postgres o Redis usa la porta: fermalo, oppure cambia la porta a sinistra in `compose.yaml` e la variabile corrispondente |
| `Validate failed: Migrations have failed validation` | È stata modificata una migrazione già applicata: ripristinala e crea una nuova versione; in locale puoi ripartire da zero con `docker compose down -v` |
