package connectfour.model;

/**
 * A játék folyamatának fázisai (állapotai). Minden parancs megmondja, mely fázisokban
 * szabad használni, a fázisok közti átmeneteket pedig a parancsok és a
 * {@code GameEngine} végzik.
 *
 * <pre>
 *   EDIT ──NEW / LOAD──▶ HAVE_BOARD ──PLAY──▶ PLAYING ──(négyes vagy tele tábla)──▶ GAME_ENDED
 *                         ▲  │ PUT / TAKE      │ DROP / MACHINE                     │   │
 *                         └──┘                 ◀──────────── UNDO ─────────────────┘   │ SAVE
 *                                                                                        ▼
 *   LOAD színnel mentett, folytatható játszmára: rögtön PLAYING              SAVED_GAME
 *   RESTART: bármelyik táblás fázisból vissza EDIT-be
 * </pre>
 */
public enum GamePhase {

    EDIT,          // még nincs tábla: újat kell kérni (NEW) vagy betölteni (LOAD)
    HAVE_BOARD,    // van tábla, szerkeszthető (PUT/TAKE), még nem választottunk színt
    PLAYING,       // folyik a játszma
    GAME_ENDED,    // valaki nyert, vagy döntetlen lett
    SAVED_GAME     // a befejezett játszmát elmentettük
}
