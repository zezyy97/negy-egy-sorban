# Négy a sorban (Connect Four)

Konzolos „potyogós amőba” Java 21-ben: a játékos a gép ellen játszik, felváltva dobnak korongokat
egy függőleges táblába, és aki előbb kirak **négy egyforma korongot egy vonalban** (vízszintesen,
függőlegesen vagy átlósan), az nyer.

A részletes magyarázat (architektúra, minták, algoritmusok):
[MAGYARAZAT.md](MAGYARAZAT.md)

## Futtatás

**Kell hozzá:** JDK 21 és Maven (az IntelliJ IDEA mindkettőt tartalmazza).

### IntelliJ IDEA-ban
1. *File → Open…* → a projekt mappájában a `pom.xml` → *Open as Project*.
2. Várd meg, amíg a Maven letölti a függőségeket (jobb alul a folyamatjelző).
3. Nyisd meg a `src/main/java/connectfour/Main.java` fájlt, és a zöld ▶ gombbal indítsd.

### Parancssorból
```bash
mvn test                 # a tesztek futtatása
mvn verify               # tesztek + lefedettségi riport: target/site/jacoco/index.html
mvn clean install        # teljes build: checkstyle, tesztek, jacoco, függőségekkel egybecsomagolt JAR
mvn compile exec:java    # a játék indítása
java -jar target/connectfour-1.0-SNAPSHOT-jar-with-dependencies.jar [kezdoallapot.txt]
```

### Kezdőállapot külső fájlból
Indításkor a program a kezdőállást szövegfájlból olvassa be. Argumentum nélkül a JAR-ba csomagolt
`src/main/resources/start.txt` kerül betöltésre, egy saját fájlt pedig az első argumentumként lehet megadni.
Formátum: `#` jelű megjegyzés sorok, opcionális `HUMAN=R|Y` sor, majd a tábla soronként (R / Y / `.`).

### Naplózás és kódstílus
A program a `connectfour.log` fájlba naplóz (java.util.logging). A Checkstyle (`config/checkstyle.xml`)
a `validate` fázisban fut, és figyelmeztet, de nem töri el a buildet.

> Ha Windows parancssorban furcsa karakterek jelennek meg az ékezetek helyén, futtasd előbb:
> `chcp 65001`. Az IntelliJ konzoljában ez nem fordul elő.

### Adatbázis böngészőből
Indításkor elindul a H2 webkonzol: <http://localhost:8082>
JDBC URL: `jdbc:h2:mem:connectfour;DB_CLOSE_DELAY=-1`, felhasználó: `sa`, jelszó: üres.
A tábla neve `SAVED_GAME_CONNECT_FOUR`.

---

## Parancsok

A kis- és nagybetű mindegy, az oszlopokat **1-től** számozzuk. A `HELP` mindig az adott
fázisban használható parancsokat mutatja.

| Parancs | Mit csinál | Mikor használható |
|---|---|---|
| `HELP` | súgó az aktuális fázishoz | mindig |
| `PRINT` | állapot és tábla kiírása | mindig |
| `EXIT` | kilépés | mindig |
| `NEW [sorok oszlopok]` | új üres tábla (alapból 6×7, méret 4–10) | EDIT, HAVE_BOARD |
| `LIST` | mentések listája | EDIT, HAVE_BOARD, SAVED_GAME |
| `LOAD <id>` | mentés betöltése | EDIT, HAVE_BOARD, SAVED_GAME |
| `PUT <oszlop> <R\|Y>` | szerkesztés: korong bedobása | HAVE_BOARD |
| `TAKE <oszlop>` | szerkesztés: felső korong kivétele | HAVE_BOARD |
| `LEVEL <EASY\|HARD>` | gépi nehézség (véletlen / okos) | EDIT, HAVE_BOARD, PLAYING |
| `PLAY <R\|Y>` | színválasztás, indulás (a piros kezd) | HAVE_BOARD |
| `DROP <oszlop>` vagy csak `<oszlop>` | a te lépésed | PLAYING |
| `MACHINE` vagy `M` | a gép lép | PLAYING |
| `HINT` | tipp, hova érdemes dobnod | PLAYING |
| `UNDO` | visszavonás (a te lépésed + a gép válasza) | PLAYING, GAME_ENDED |
| `SAVE <név>` | mentés az adatbázisba | HAVE_BOARD, PLAYING, GAME_ENDED |
| `RESTART` | vissza a kezdéshez | HAVE_BOARD, PLAYING, GAME_ENDED, SAVED_GAME |

Színek: `R` vagy `P` = piros, `Y` vagy `S` = sárga.

## Példa játékmenet

```
[EDIT] Parancsok: HELP, PRINT, EXIT, NEW, LIST, LOAD, LEVEL
> LOAD 2
Betöltve: "piros-nyerhet" (6×7)
  1  2  3  4  5  6  7
| .  .  .  .  .  .  . |
| .  .  .  .  .  .  . |
| .  .  .  .  .  .  . |
| .  .  .  .  .  .  . |
| Y  Y  Y  .  .  .  . |
| R  R  R  .  .  .  . |
+---------------------+
Folytatjuk a játszmát – te vagy a piros.

[PLAYING] Parancsok: HELP, PRINT, EXIT, LEVEL, DROP, MACHINE, HINT, UNDO, SAVE, RESTART
Te jössz (Piros) – írd be az oszlop számát, pl. 4
> 4
Bedobtad a korongot a 4. oszlopba.
  ...
|(R)(R)(R)(R) .  .  . |
+---------------------+

*** Piros nyert! Gratulálok, legyőzted a gépet! ***
```

Előre betöltött mentések: `1` üres tábla, `2` egy lépésre a győzelemtől, `3` blokkolnod kell,
`4` kisebb, 5×6-os félbehagyott játszma.

---

## Szerkezet

```
src/main/java/connectfour/
├── Main.java                     belépési pont, összeköti a részeket
├── model/                        adatok: Board (immutable), Disc, Position, Move, GameState, GamePhase…
├── businesslogic/                szabályok: BoardService (ki jön, szabályos-e, ki nyert)
│   └── strategy/                 gépi játékos: MoveStrategy + Random / Smart (Strategy minta)
├── command/                      Command ősosztály, CommandRegistry, 16 parancs
├── engine/                       GameEngine: beolvasás → parancs → végrehajtás → játszma vége?
└── db/                           DbInit, GameRepository (DAO), SavedGame
src/main/resources/createDbScript.sql   adatbázis-séma + 4 mintajátszma
src/test/java/connectfour/              JUnit 5 tesztek
```
