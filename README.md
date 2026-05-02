# IceCore

Plugin Minecraft sviluppato come progetto personale.

È strutturato come un progetto professionale: multi-modulo e asincrono.

---

## Stack

- **Java 21**
- **Paper 1.21.1** — API Bukkit/Paper
- **PostgreSQL 16** — dati relazionali e persistenti
- **Redis 7** — cache, stato volatile, richieste con TTL automatico
- **Flyway** — migration dello schema SQL versionate
- **HikariCP** — connection pooling SQL
- **Jedis** — connection pooling Redis
- **Jackson** — serializzazione JSON per la cache Redis
- **Gradle multi-modulo** con version catalog

---

## Struttura del progetto

Il progetto è diviso in quattro moduli:

```
common   →   nessuna dipendenza esterna
core     →   dipende da common
paper    →   dipende da core e common
velocity →   dipende da core e common (in sviluppo)
```

`core` non ha Paper nel classpath. Tutta la logica di business compila e gira senza un server Minecraft.

```
common/
└── model/          → PlayerProfile, Friendship (record immutabili)
    enums/          → FriendActionResult
    exception/      → IceCoreException, DatabaseException, RedisException

core/
└── api/service/    → PlayerService, FriendService (interfacce pubbliche)
    cache/          → PlayerCache, FriendRequestCache
    config/         → DatabaseConfig, RedisConfig (record con validazione)
    database/
    ├── sql/        → SqlConnectionProvider (HikariCP), FlywayMigrator
    └── redis/      → RedisConnectionProvider (JedisPool)
    repository/sql/ → PlayerRepository, FriendshipRepository
    service/impl/   → PlayerServiceImpl, FriendServiceImpl

paper/
└── command/        → BaseCommand, ProfileCommand, PlaytimeCommand,
    │                 FriendCommand, LangCommand
    config/         → PluginConfigLoader
    lang/           → LangService, Lang, LocaleCache
    listener/       → PlayerJoinListener
    util/           → DurationFormatter
```

---

## Perché Redis e non solo Postgres

**Postgres** gestisce i dati che devono sopravvivere ai riavvii e hanno struttura relazionale: profili, amicizie e prossimamente ban ed economia. Flyway tiene traccia di ogni modifica allo schema con file SQL versionati. Quando rilascio una nuova versione del plugin, le migration vengono applicate automaticamente all'avvio senza toccare il database a mano.

**Redis** gestisce tre cose diverse:

- **Cache dei profili** — pattern cache-aside con TTL di 30 minuti. Al join il profilo viene caricato da Postgres e messo in cache. Le letture successive non toccano il database. Se Redis non risponde, il sistema fa fallback su Postgres senza errori visibili.

- **Richieste di amicizia in attesa** — ogni richiesta è una chiave Redis con TTL di 5 minuti. Quando il TTL scade, la richiesta decade da sola. Non c'è nessun job di pulizia. Il TTL fa il lavoro.

- **Cache locale per-player** — la lingua scelta dal giocatore viene tenuta in una `ConcurrentHashMap` in memoria, popolata al join e svuotata al quit. Nessuna query SQL per ogni messaggio mostrato.

---

## Flyway — gestione dello schema

Ogni modifica al database è un file SQL nella cartella `core/src/main/resources/db/migration/`:

```
V1__create_players_table.sql
V2__add_player_locale.sql
V3__create_friendships_table.sql
```

All'avvio del plugin, Flyway confronta i file sul classpath con la tabella `flyway_schema_history` nel database e applica solo quelli nuovi. Un server che gira la v0.1 e aggiorna alla v0.3 riceve le migration V2 e V3 automaticamente, nell'ordine corretto, senza intervento manuale.

---

## Modello asincrono

Il main thread di Minecraft non fa I/O. Mai.

Ogni chiamata al database gira su un `ExecutorService` dedicato (`IceCore-DB-Worker`) e ritorna un `CompletableFuture`. Le interfacce del service layer espongono solo `CompletableFuture<T>` — non è possibile chiamarle in modo sincrono e aspettarsi un risultato diretto.

```java
playerService.handleJoin(uuid, username)
    .thenAccept(profile -> plugin.getServer().getScheduler().runTask(plugin, () -> {
        // torna sul main thread solo per toccare il world/player
        Lang.setLocale(uuid, profile.locale());
        player.sendMessage(Lang.get(player, "join.welcome", "player", profile.username()));
    }))
    .exceptionally(throwable -> {
        plugin.getLogger().warning("Failed to load profile for " + username);
        return null;
    });
```

Quindi async per il database, sync per il world. Il `runTask` è il confine tra i due mondi.

---

## Fault tolerance

**Redis non risponde**: le letture dalla cache ritornano `Optional.empty()` (miss), le scritture loggano un warning e continuano. Il server è più lento, non rotto.

**Postgres non risponde**: il plugin non si avvia. Senza storage persistente non c'è modalità degradata sensata — meglio fallire subito che girare in uno stato inconsistente.

**Shutdown con giocatori online**: `onDisable()` chiama `handleQuit()` per ogni giocatore connesso e aspetta fino a 5 secondi che tutti i salvataggi finiscano prima di chiudere i connection pool. Nessun playtime viene perso su uno shutdown pulito.

---

## Funzionalità attuali

**`/profile [player]`** — mostra UUID, primo accesso, ultimo accesso e playtime totale. Se il giocatore è online, il playtime include la sessione corrente in tempo reale.

**`/playtime [player]`** — playtime con sessione live per i giocatori online.

**`/friend <add|accept|deny|remove|list|requests> [player]`** — sistema amici completo. Le richieste in attesa vivono in Redis con TTL automatico, le amicizie persistono su Postgres.

**`/lang [locale]`** — ogni giocatore sceglie la propria lingua. La preferenza viene salvata sul profilo e ricaricata al join. I file di traduzione vengono estratti automaticamente dal JAR alla prima esecuzione.

---

## Avvio rapido

**Prerequisiti**: Java 21, Paper 1.21.1+, PostgreSQL 16+, Redis 7+

```bash
# database locali con Docker
docker run -d --name icecore-postgres \
  -e POSTGRES_PASSWORD=icecore -e POSTGRES_DB=icecore \
  -p 5432:5432 postgres:16

docker run -d --name icecore-redis \
  -p 6379:6379 redis:7-alpine

# build
./gradlew :paper:shadowJar
```

Copia il JAR in `plugins/`, avvia il server. La prima volta vengono estratti `config.yml` e i file di lingua in `plugins/IceCore/`. Modifica le credenziali del database e riavvia.

Le migration SQL vengono applicate in automatico.
