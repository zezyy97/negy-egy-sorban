# Négy a sorban – részletes magyarázat

Ez a leírás végigvezet a projekten úgy, hogy a végére minden osztályról el tudd mondani, mit csinál
és miért így van megírva. A végén van egy **védési kérdéssor** és néhány **élő módosítási feladat**,
mert ilyesmit szoktak kérni a bemutatáskor.

Tartalom:

1. [A játék szabályai](#1-a-játék-szabályai)
2. [Mi felel meg minek a példaprojektben?](#2-mi-felel-meg-minek-a-példaprojektben)
3. [Rétegek és csomagok](#3-rétegek-és-csomagok)
4. [A Command tervezési minta](#4-a-command-tervezési-minta)
5. [Fázisok (állapotgép)](#5-fázisok-állapotgép)
6. [A megváltoztathatatlan tábla és a visszavonás](#6-a-megváltoztathatatlan-tábla-és-a-visszavonás)
7. [Szabályok és győzelemkeresés – BoardService](#7-szabályok-és-győzelemkeresés--boardservice)
8. [A gépi játékos – Strategy minta](#8-a-gépi-játékos--strategy-minta)
9. [Adatbázis – H2 és Repository](#9-adatbázis--h2-és-repository)
10. [A motor – GameEngine](#10-a-motor--gameengine)
11. [Tesztek](#11-tesztek)
12. [Miben lép tovább a példához képest?](#12-miben-lép-tovább-a-példához-képest)
15. [Osztályok egy mondatban](#13-osztályok-egy-mondatban)

---

## 1. A játék szabályai

- A tábla alapból 6 sor × 7 oszlop (a `NEW 5 6` paranccsal 4–10 között bármekkora lehet).
- A két szín: **piros (R)** és **sárga (Y)**. Mindig a **piros kezd**, utána felváltva lépnek.
- Lépni úgy lehet, hogy egy oszlopba bedobsz egy korongot – az a **legalsó szabad mezőre esik**
  (gravitáció). Teli oszlopba nem lehet dobni.
- Aki előbb kirak **négy egyforma korongot egy vonalban** (vízszintesen, függőlegesen vagy a két
  átló valamelyike mentén), az nyer.
- Ha megtelik a tábla és senkinek nincs négyese: **döntetlen**.

A játékos a gép ellen játszik. `PLAY R` esetén te vagy a piros (te kezdesz), `PLAY Y` esetén a gép.

---

## 2. Mi felel meg minek a példaprojektben?

A projekt szándékosan ugyanazt a vázat használja, mint a *Fox and Hounds* példa, így a két
program rétegről rétegre összevethető:

| Fox and Hounds (példa) | Négy a sorban (ez a projekt) | Megjegyzés |
|---|---|---|
| `Board` (immutable) | `model.Board` (immutable) | itt 2D tömb, a példában pozíciólisták |
| `Position` osztály | `model.Position` **record** | a record ugyanazt adja sokkal rövidebben |
| `Direction` (4 átlós irány) | `model.LineDirection` (4 vonalirány) | itt a győzelemkereséshez kell |
| `Fox`, `Hound`, `Figure` | `model.Disc` enum | egyforma korongok vannak, csak a színük más |
| `Move` (bábu + irány) | `model.Move` record (szín + oszlop) | gravitáció miatt elég az oszlop |
| `GamePhase` | `model.GamePhase` | EDIT, HAVE_BOARD, PLAYING, GAME_ENDED, SAVED_GAME |
| `GameState` | `model.GameState` | + előzményverem (UNDO), + gépi stratégia |
| `BoardService` | `businesslogic.BoardService` | szabályok: ki jön, szabályos-e, ki nyert |
| `doARandom…Move` | `strategy.RandomMoveStrategy`, `SmartMoveStrategy` | Strategy minta, cserélhető |
| `Command` + `CommandRegistry` | `command.Command` + `CommandRegistry` | + usage/leírás a súgóhoz |
| 18 parancsosztály | 16 parancsosztály | néhányat egy paraméteres parancs vált ki |
| `GameEngine` | `engine.GameEngine` | bemenet/kimenet kívülről adható (tesztelhető) |
| `DbInit` + SQL a parancsokban | `db.DbInit` + `db.GameRepository` | az SQL egy helyre került (DAO) |
| `createDbScript.txt` | `createDbScript.sql` | + megszorítások (CHECK), + mintajátszmák |

---

## 3. Rétegek és csomagok

```
                ┌────────────────────────────────────────────┐
  felhasználó → │ engine.GameEngine  (beolvas, parancsot keres,│
                │                     végrehajt, figyeli a végét)│
                └───────────────┬────────────────────────────┘
                                │ execute(state, args, out)
                ┌───────────────▼────────────────────────────┐
                │ command.*  (16 parancs + CommandRegistry)    │──────► db.GameRepository ─► H2
                └───────────────┬────────────────────────────┘          (LIST, LOAD, SAVE)
                                │ módosítja
                ┌───────────────▼────────────────────────────┐
                │ model.GameState  (fázis, színek, előzmények) │
                └───────────────┬────────────────────────────┘
                                │ használja
                ┌───────────────▼────────────────────────────┐
                │ businesslogic.BoardService  (szabályok)      │◄── strategy.* (gépi játékos)
                └───────────────┬────────────────────────────┘
                                │ dolgozik rajta
                ┌───────────────▼────────────────────────────┐
                │ model.Board  (immutable tábla) + Disc, Position, Move…│
                └────────────────────────────────────────────┘
```

A szabály: **minden réteg csak az alatta lévőt ismeri.** A `Board` semmit nem tud a parancsokról,
a `BoardService` semmit nem tud a konzolról, a parancsok semmit nem tudnak az SQL-ről.
Ettől lesz minden rész külön tesztelhető.

---

## 4. A Command tervezési minta

### Mi a lényege?

Egy kérést (itt: egy begépelt parancsot) **önálló objektumba csomagolunk**. Így a kérés
„kiadója” (a motor) és a „végrehajtója” (az állapot) nem függ egymástól, és új kérésfajtát
anélkül vehetünk fel, hogy a meglévő kódhoz hozzányúlnánk.

### A minta szereplői ebben a projektben

| Szerep a mintában | Itt | Feladata |
|---|---|---|
| **Command** (közös felület) | `command.Command` absztrakt osztály | `execute(...)` + regex + engedélyezett fázisok |
| **ConcreteCommand** | `DropCommand`, `UndoCommand`, `SaveCommand`… | egy-egy konkrét művelet |
| **Invoker** (meghívó) | `engine.GameEngine` | kiválasztja és meghívja a parancsot, de nem tudja, mit csinál |
| **Receiver** (fogadó) | `model.GameState` (+ `BoardService`) | ezen dolgoznak a parancsok |
| **Client** (összeállító) | `command.CommandRegistry` | létrehozza és nyilvántartja a parancsobjektumokat |

### Mit tud magáról egy parancs?

```java
public class DropCommand extends Command {

    public DropCommand() {
        super("DROP",                                   // név (a listában ez látszik)
              "DROP <oszlop>  (vagy csak a szám)",      // használat (HELP)
              "Korong bedobása a megadott oszlopba.",   // leírás (HELP)
              "(?:DROP\\s+)?(\\d{1,2})",                // minta: "DROP 4" vagy csak "4"
              Set.of(GamePhase.PLAYING));               // mikor használható
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        // args.group(1) = a beírt szám
        ...
    }
}
```

A regexben a zárójeles csoportok emelik ki a paramétereket (`args.group(1)`), a `(?: … )?`
pedig egy *nem rögzítő, elhagyható* csoport: ettől fogadja el a parancs a `DROP 4` és a `4`
alakot is. Az ősosztály `CASE_INSENSITIVE` módban fordítja a mintát, ezért a kis- és nagybetű mindegy.

### Egy parancs útja – mi történik, ha beírod, hogy `4`?

1. `GameEngine.run()` beolvassa a sort: `"4"`.
2. `CommandRegistry.findMatchingCommand("4", PLAYING)` végigmegy a parancsokon: az első, amelyik
   PLAYING-ben engedélyezett **és** illeszkedik, a `DropCommand`. Visszaadja a parancsot és a
   `Matcher`-t együtt (`MatchResult`).
3. A motor meghívja: `dropCommand.execute(state, matcher, out)`.
4. A `DropCommand` ellenőrzi, hogy tényleg a játékos jön-e, és hogy létező, nem teli oszlop-e.
5. `state.playMove(new Move(RED, 3))` → a `GameState` elteszi az előző táblát a verembe, majd
   `BoardService.doMove(...)` → `Board.drop(3, RED)` egy **új** táblát ad.
6. A parancs kirajzolja a táblát.
7. A motor `autoDetectEndOfGame()`-mel megnézi, vége lett-e (négyes vagy tele tábla) → ha igen,
   GAME_ENDED fázis.

### Miért jó ez?

- **Nyitott–zárt elv:** új parancshoz nem kell a motort módosítani, csak egy új osztály
  + egy sor a `CommandRegistry`-ben (lásd [14. fejezet](#14-élő-módosítási-feladatok-gyakorlásra)).
- **A súgó magától készül:** a `HelpCommand` csak végigmegy a parancsokon és kiírja a
  `getUsage()` / `getDescription()` értékeket.
- **Fázisfüggő engedélyezés egy helyen:** a parancs maga mondja meg, mikor használható;
  a motor ezért tudja megkülönböztetni az elgépelést („Ismeretlen parancs…”) a rossz időben
  kiadott parancstól („A DROP parancsot most (EDIT fázisban) nem lehet használni”).
- **Tesztelhetőség:** minden parancs külön is futtatható egy `GameState`-tel (`CommandsTest`).

---

## 5. Fázisok (állapotgép)

```
  EDIT ──NEW / LOAD──▶ HAVE_BOARD ──PLAY──▶ PLAYING ──(négyes vagy tele tábla)──▶ GAME_ENDED
                         ▲  │ PUT / TAKE      │ DROP / MACHINE                     │   │
                         └──┘                 ◀──────────── UNDO ─────────────────┘   │ SAVE
                                                                                        ▼
  LOAD színnel mentett, folytatható játszmára: rögtön PLAYING              SAVED_GAME
  RESTART: bármelyik táblás fázisból vissza EDIT-be
```

| Fázis | Jelentése | Ki/mi viszi tovább |
|---|---|---|
| `EDIT` | még nincs tábla | `NEW` vagy `LOAD` → HAVE_BOARD (vagy PLAYING) |
| `HAVE_BOARD` | van tábla, szerkeszthető, nincs még szín | `PLAY` → PLAYING |
| `PLAYING` | folyik a játszma | a **motor** → GAME_ENDED, ha vége |
| `GAME_ENDED` | valaki nyert vagy döntetlen | `SAVE` → SAVED_GAME, `UNDO` → PLAYING |
| `SAVED_GAME` | elmentett befejezett játszma | `RESTART` → EDIT, `LOAD` → HAVE_BOARD/PLAYING |

Melyik parancs melyik fázisban él:

| Parancs | EDIT | HAVE_BOARD | PLAYING | GAME_ENDED | SAVED_GAME |
|---|:-:|:-:|:-:|:-:|:-:|
| HELP, PRINT, EXIT | ✔ | ✔ | ✔ | ✔ | ✔ |
| NEW | ✔ | ✔ | | | |
| LIST, LOAD | ✔ | ✔ | | | ✔ |
| PUT, TAKE | | ✔ | | | |
| LEVEL | ✔ | ✔ | ✔ | | |
| PLAY | | ✔ | | | |
| DROP, MACHINE, HINT | | | ✔ | | |
| UNDO | | | ✔ | ✔ | |
| SAVE | | ✔ | ✔ | ✔ | |
| RESTART | | ✔ | ✔ | ✔ | ✔ |

Fontos: azt, hogy **ki következik**, nem a fázis tárolja és nem is egy külön változó, hanem a
`BoardService.nextToMove()` számolja ki a korongok számából (lásd 7. fejezet). Ezért a DROP és a
MACHINE parancs ugyanabban a fázisban él, de mindkettő ellenőrzi, hogy most ő következik-e.

---

## 6. A megváltoztathatatlan tábla és a visszavonás

A `Board` osztály **immutable**: minden mezője `final`, a belső `Disc[][]` tömböt soha nem adja
ki, és nincs egyetlen olyan metódusa sem, ami a meglévő táblát módosítaná. A „módosítók” új
táblát adnak vissza:

```java
Board b1 = Board.standard();
Board b2 = b1.drop(3, Disc.RED);   // b1 továbbra is üres, b2-ben van egy piros korong
```

Ehhez a `drop` lemásolja a tömböt (`copyCells()`), a másolatba ír, és abból készít új `Board`-ot
egy privát konstruktorral. A `fromDescription` is másolatot tart meg, így kívülről semmi nem
„nyúlhat bele” a táblába.

Három dolog lesz ettől egyszerű:

1. **Visszavonás (UNDO).** A `GameState` minden lépés előtt egy verembe (`ArrayDeque<Board>`)
   teszi az előző táblát. Visszavonáskor csak leveszi a verem tetejét. Nem kell „visszafelé
   lépést” programozni, és nem lehet elrontani.
2. **A gép „próbálgathat”.** A `SmartMoveStrategy` kipróbálja, mi lenne, ha ide vagy oda dobna
   (`board.drop(...)`), de ettől az igazi állás nem változik.
3. **Biztonságos megosztás.** Ugyanazt a `Board`-ot oda lehet adni a kirajzolásnak, a mentésnek,
   a stratégiának – egyik sem ronthatja el a másiknak.

Az UNDO addig lép vissza, amíg újra a **játékos** jön: ha a gép már válaszolt, a gép lépését és
a játékosét is visszavonja (különben a visszavonás után a gép jönne, és ugyanazt lépné).

---

## 7. Szabályok és győzelemkeresés – BoardService

### Ki jön?

```java
public Disc nextToMove() {
    return board.count(Disc.RED) > board.count(Disc.YELLOW) ? Disc.YELLOW : Disc.RED;
}
```

Mivel a piros kezd, egy szabályos állásban vagy ugyanannyi piros és sárga korong van (a piros
jön), vagy eggyel több piros (a sárga jön). Ezt az `isDiscCountConsistent()` ellenőrzi – szerkesztés
után ugyanis elromolhat, és ilyenkor a `PLAY` nem engedi elindítani a játékot.

**Miért nem tároljuk?** Mert a származtatott adat sosem kerülhet ellentmondásba azzal, amiből
származik. A példaprojektben a „ki lép” két helyen is szerepelt (`Board.isFoxOnMove` és
`GameState.foxIsOnMove`), és a kettő el tudott térni egymástól.

### Van-e győztes? – `findWinningPositions`

Minden korongtól elindulunk a 4 irány mindegyikébe, és megnézzük, a következő 3 mező is
ugyanolyan színű-e:

```
LineDirection: HORIZONTAL (0, +1)  —     VERTICAL (+1, 0)  |
               DIAGONAL_DOWN (+1, +1) \  DIAGONAL_UP (-1, +1) /
```

Miért elég 4 irány, és nem kell 8? Mert minden vonalat a bal (vagy a felső) végéről is elérünk:
a „balra” irány ugyanazt a négyest találná meg, mint a „jobbra”, csak a másik végéről.
Egy 6×7-es táblán ez legfeljebb 42 × 4 = 168 vizsgálat, vagyis bőven elég gyors.

A metódus az összes nyerő mezőt visszaadja (nem csak igaz/hamis), így a kirajzolás zárójelekkel
ki tudja emelni a nyerő négyest: `|(R)(R)(R)(R) .  .  . |`.

### Nyerne-e ez a lépés? – `isWinningMove`

A gépi játékosnak sokszor kell megkérdeznie: „ha ide dobnék, nyernék?”. Erre nem az egész táblát
vizsgáljuk, csak a **most lerakott korongon átmenő** négy vonalat: mindkét irányba megszámoljuk
az azonos színű szomszédokat, és ha `1 + balra + jobbra ≥ 4`, nyerő a lépés. Így az `R R _ R`
helyzetet is felismeri (a résbe dobott korong köti össze a két részt).

### Indítható-e a játék? – `problemBeforePlay`

`Optional<String>`-et ad vissza: üres, ha minden rendben, különben a hiba magyar leírását
(rossz a korongarány / már van négyes / tele a tábla). Az `Optional` jobb a `null`-nál, mert a
típus maga jelzi, hogy lehet, hogy nincs érték.

---

## 8. A gépi játékos – Strategy minta

A **Strategy minta** lényege: egy algoritmuscsaládot közös felület mögé teszünk, és futás közben
cserélhetővé tesszük. Itt az algoritmus az, hogy *a gép melyik oszlopot választja*.

```java
public interface MoveStrategy {
    String getName();
    int chooseColumn(Board board, Disc me);
}
```

- `RandomMoveStrategy` – **könnyű**: véletlen nem teli oszlop (mint a példában a gép).
- `SmartMoveStrategy` – **nehéz** (alapértelmezett), sorban megnézi:
  1. **Nyerhetek most?** Ha igen, azt lépem.
  2. **Nyerhet az ellenfél?** Ha igen, oda dobok (blokkolok).
  3. **Ne adjak ajándékot!** Kiszűröm azokat az oszlopokat, amelyek után az ellenfél azonnal
     nyerne (pl. ha pont az ő nyerő mezője alá dobnék, és így „alátámasztanám”).
  4. **Középre!** A maradékból a középhez legközelebbit választom, mert a középső oszlopon megy át
     a legtöbb lehetséges négyes. Egyenlőség esetén véletlenszerűen.

A `MachineCommand` csak ennyit tud: `state.getMachineStrategy().chooseColumn(...)`. Hogy ez
melyik osztály, azt a `LEVEL` parancs dönti el. A `HintCommand` ugyanazt az okos stratégiát
használja a játékos színével – ugyanaz a kód két célra.

A stratégiák `Random` objektumot kaphatnak a konstruktorban: tesztben rögzített „seed”-del a
véletlen is megismételhető. Egy teszt (`smartBeatsRandom`) 20 játszmát játszat le az okos és a
véletlen stratégia között, és ellenőrzi, hogy az okos legalább 18-at megnyer.

---

## 9. Adatbázis – H2 és Repository

### H2 memóriában

A `DbInit.initDb(url)` létrehoz egy **memóriában futó** H2 adatbázist (`jdbc:h2:mem:connectfour`),
és lefuttatja rajta a `createDbScript.sql` szkriptet. A `DB_CLOSE_DELAY=-1` miatt az adatbázis
addig él, amíg a program fut (különben az utolsó kapcsolat bezárásakor eltűnne). Kilépéskor a
mentések elvesznek – ez a példaprojektben is így volt, a cél a JDBC-használat bemutatása.

### A tábla

```sql
CREATE TABLE SAVED_GAME_CONNECT_FOUR (
    ID                INT AUTO_INCREMENT PRIMARY KEY,
    NAME              VARCHAR(20)  NOT NULL UNIQUE,
    ROW_COUNT         INT          NOT NULL,
    COL_COUNT         INT          NOT NULL,
    BOARD_DESCRIPTION VARCHAR(100) NOT NULL,     -- pl. ".......…YYY....RRR...."
    HUMAN_DISC        CHAR(1),                    -- 'R', 'Y' vagy NULL
    SAVED_AT          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT CHK_SIZE  CHECK (ROW_COUNT BETWEEN 4 AND 10 AND COL_COUNT BETWEEN 4 AND 10),
    CONSTRAINT CHK_DESC  CHECK (LENGTH(BOARD_DESCRIPTION) = ROW_COUNT * COL_COUNT),
    CONSTRAINT CHK_HUMAN CHECK (HUMAN_DISC IS NULL OR HUMAN_DISC IN ('R', 'Y'))
);
```

A táblát egyetlen szövegként tároljuk: soronként felülről lefelé, `R` = piros, `Y` = sárga,
`.` = üres (`Board.toDescription()` / `Board.fromDescription()`). Betöltéskor a `fromDescription`
ellenőrzi a hosszt, a karaktereket és a gravitációt (nincs-e „lebegő” korong), így hibás adatból
nem lesz hibás tábla. Az adatbázis `CHECK` megszorításai ugyanezt a másik oldalról védik.

**Nem tároljuk, ki következik** – ezt a korongokból kiszámoljuk (7. fejezet).

### Repository (DAO) minta

Minden SQL a `GameRepository` osztályban van: `findAll()`, `findById(id)`, `existsByName(name)`,
`save(name, board, humanDisc)`. A parancsok csak ezeket hívják, SQL-t nem látnak.

- Ha egyszer fájlba vagy igazi adatbázis-szerverre mentenénk, csak ezt az egy osztályt kellene cserélni.
- A `CommandRegistry` konstruktorban kapja meg a repository-t (**függőséginjektálás**), ezért a
  tesztek minden esetben saját, friss adatbázist adhatnak (`TestBoards.freshDatabase()`).
- Minden lekérdezés **`PreparedStatement`**-tel, `?` paraméterekkel megy, így a felhasználó által
  beírt név soha nem válik SQL kóddá (**SQL injection** elleni védelem – erre teszt is van).
- A `try-with-resources` (`try (Connection conn = …)`) automatikusan lezárja a kapcsolatot,
  az utasítást és az eredményhalmazt, akkor is, ha kivétel történik.
- Az `SQLException`-t `DataAccessException`-be (nem ellenőrzött kivétel) csomagoljuk, így a
  parancsoknak nem kell `throws SQLException`-t írniuk, de a hiba oka (`getCause()`) megmarad.

### Webkonzol

A `DbInit.startWebConsole(8082)` elindítja a H2 böngészős felületét (<http://localhost:8082>),
ahol futás közben SQL-lel is megnézheted a mentéseket. Ha a port foglalt, a játék ettől még megy.

---

## 10. A motor – GameEngine

```java
while (!state.isExitRequested()) {
    showPrompt();                                     // fázis + parancsok + ki jön
    if (!in.hasNextLine()) break;                     // elfogyott a bemenet → rendes leállás
    String input = in.nextLine().trim();
    Optional<MatchResult> match = registry.findMatchingCommand(input, state.getPhase());
    if (match.isEmpty()) { explainWhyNotMatched(input); continue; }
    try {
        match.get().command().execute(state, match.get().matcher(), out);
    } catch (RuntimeException e) {
        out.println("Váratlan hiba: " + e.getMessage());   // egy hiba ne állítsa le a programot
    }
    autoDetectEndOfGame();                            // négyes vagy tele tábla → GAME_ENDED
}
```

Három tervezési döntés:

- **A bemenet és a kimenet kívülről jön** (`InputStream`, `PrintStream` a konstruktorban), nem
  fixen `System.in` / `System.out`. Így a `GameEngineTest` előre megírt sorokkal teljes
  játszmákat tud lejátszani, és a kimenetet ellenőrzi.
- **A játszma végét a motor figyeli**, nem az egyes parancsok. Így bármelyik parancs okozza a
  győzelmet (DROP vagy MACHINE), egy helyen dől el, mi történik utána.
- **Segítőkész hibaüzenet:** ha a szöveg egy másik fázisban létező parancs, ezt meg is mondja.

A `Main` csak „összedrótozza” a részeket: adatbázis → repository → registry → motor.

---

## 11. Tesztek

87 JUnit 5 teszt, 7 osztályban (`mvn test`, lefedettség: `mvn verify` → `target/site/jacoco/index.html`):

| Tesztosztály | Mit ellenőriz |
|---|---|
| `BoardTest` | gravitáció, teli oszlop, immutability, szöveges mentés oda-vissza, lebegő korong elutasítása, kirajzolás |
| `DiscAndTextTest` | színek, betűjelek, `GameResult`, `Position`, magyar névelők („az 5.”, „a 4.”) |
| `BoardServiceTest` | ki jön, szabálytalan lépések, mind a 4 győzelmi irány, döntetlen, `isWinningMove`, indíthatóság |
| `MoveStrategyTest` | az okos gép nyer, blokkol, nem ad ajándékot, középre dob; okos vs. véletlen 20 játszma |
| `CommandRegistryTest` | illeszkedés, kis-/nagybetű, fázisszűrés, **nincs kétértelmű bemenet** |
| `CommandsTest` | minden parancs: fázisátmenetek, körsorrend, UNDO, mentés/betöltés, hibaüzenetek |
| `GameRepositoryTest` | mintajátszmák, mentés-visszaolvasás, egyedi név, SQL injection elleni védelem |
| `GameEngineTest` | teljes játszmák forgatókönyvből: játékos nyer, gép nyer, hibaüzenetek, bemenet vége |

A tesztekben a táblákat „rajzolva” adjuk meg, így ránézésre érthetők:

```java
Board board = TestBoards.of(
        ".......",
        ".......",
        "YYY....",
        "RRR....");
assertTrue(BoardService.isWinningMove(board, 3, Disc.RED));
```

---

## 12. Miben lép tovább a példához képest?

Ezek nem kötelező elemek, de a bemutatáskor jól mutatnak, mert mindegyik mögött egy tudatos
tervezési döntés áll. (A példaprojekt oktatási célú, szándékosan egyszerűbb – ezt érdemes
tapintatosan, „továbbfejlesztésként” megfogalmazni.)

| A példában | Itt |
|---|---|
| A „ki lép” két helyen tárolva (`Board` és `GameState`), eltérhettek | kiszámoljuk a korongokból – nem lehet ellentmondás |
| A lépésparancsok nem ellenőrizték, hogy ki következik | DROP / MACHINE / HINT ellenőrzi |
| Minden parancs újrafordította a saját regexét, és felülírta a `match`-et | az ősosztály egyszer elintézi |
| A motor kétszer illesztette a regexet ugyanarra a bemenetre | a registry a parancsot és a `Matcher`-t együtt adja |
| SQL a parancsokban, a LIST és a LOAD betöltő kódja kétszer | egy `GameRepository` (DAO) |
| A régi `PlayOnTerminal` szövegösszefűzéssel épített SQL-t | mindenhol `PreparedStatement` |
| Elfogyó bemenetnél (`Ctrl+D`) kivétellel leállt | `hasNextLine()` – rendes leállás |
| Egy váratlan kivétel leállította a programot | a motor elkapja, kiírja, és folytatja |
| Szerkesztett, értelmetlen állásból is indítható volt a játék | a `PLAY` ellenőrzi (`problemBeforePlay`) |
| Volt olyan fázis (`HAVE_SIDE`), amelybe egyik parancs sem vitt | minden fázis elérhető és használt |
| Az egyik teszt (`testGetEmptyBoard`) `null` listát várt, de üres listát kapott | 87 zöld teszt |
| `System.out` fixen a parancsokban | `PrintStream out` paraméter – a kimenet tesztelhető |
| Nincs visszavonás | UNDO, az immutable tábla miatt pár sorból |
| Egyféle (véletlen) gép | Strategy minta, két szint + tipp |

---

## 15. Osztályok egy mondatban

| Osztály | Egy mondatban |
|---|---|
| `Main` | Létrehozza az adatbázist, a parancsokat és a motort, majd elindítja. |
| `model.Board` | Megváltoztathatatlan tábla; a `drop` / `removeTop` új táblát ad. |
| `model.Disc` | A két szín (piros, sárga), betűjellel és magyar névvel. |
| `model.Position` | Egy mező (sor, oszlop) recordként. |
| `model.LineDirection` | A 4 irány, amerre egy négyes állhat. |
| `model.Move` | Egy lépés: szín + oszlop. |
| `model.GameResult` | Tart még / piros nyert / sárga nyert / döntetlen. |
| `model.GamePhase` | Az állapotgép állapotai. |
| `model.GameState` | A futó játék változó állapota + előzményverem + gépi stratégia. |
| `model.HungarianText` | Helyes névelő a számok előtt („az 5. oszlop”). |
| `businesslogic.BoardService` | Szabályok: ki jön, szabályos-e, ki nyert, indítható-e. |
| `strategy.MoveStrategy` | A gépi játékos felülete (Strategy minta). |
| `strategy.RandomMoveStrategy` | Könnyű szint: véletlen oszlop. |
| `strategy.SmartMoveStrategy` | Nehéz szint: nyer, blokkol, nem ad ajándékot, középre húz. |
| `command.Command` | Minden parancs őse: regex, fázisok, `execute`. |
| `command.CommandRegistry` | A parancsok listája és a megfelelő parancs megkeresése. |
| `command.*Command` | Egy-egy felhasználói parancs (16 db). |
| `engine.GameEngine` | Fő ciklus: beolvas, végrehajt, figyeli a játszma végét. |
| `db.DbInit` | Létrehozza a H2 adatbázist a szkriptből, elindítja a webkonzolt. |
| `db.GameRepository` | Minden SQL egy helyen (DAO): listázás, betöltés, mentés. |
| `db.SavedGame` | Egy beolvasott mentés adatai. |
| `db.DataAccessException` | Az adatbázis-hiba nem ellenőrzött kivételként. |
