-- A mentett játszmák táblája. Indításkor a program lefuttatja ezt a szkriptet
-- a memóriában futó H2 adatbázison, így mindig ugyanabból az állapotból indulunk.

DROP TABLE IF EXISTS SAVED_GAME_CONNECT_FOUR;

CREATE TABLE SAVED_GAME_CONNECT_FOUR (
    ID                INT AUTO_INCREMENT PRIMARY KEY,
    NAME              VARCHAR(20)  NOT NULL UNIQUE,
    ROW_COUNT         INT          NOT NULL,
    COL_COUNT         INT          NOT NULL,
    -- soronként felülről lefelé: 'R' = piros, 'Y' = sárga, '.' = üres
    BOARD_DESCRIPTION VARCHAR(100) NOT NULL,
    -- a játékos színe ('R' vagy 'Y'), NULL ha még nem választott
    HUMAN_DISC        CHAR(1),
    SAVED_AT          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT CHK_SIZE  CHECK (ROW_COUNT BETWEEN 4 AND 10 AND COL_COUNT BETWEEN 4 AND 10),
    CONSTRAINT CHK_DESC  CHECK (LENGTH(BOARD_DESCRIPTION) = ROW_COUNT * COL_COUNT),
    CONSTRAINT CHK_HUMAN CHECK (HUMAN_DISC IS NULL OR HUMAN_DISC IN ('R', 'Y'))
);

-- Néhány előre elkészített állás kipróbáláshoz (LIST, majd LOAD <id>)
INSERT INTO SAVED_GAME_CONNECT_FOUR (NAME, ROW_COUNT, COL_COUNT, BOARD_DESCRIPTION, HUMAN_DISC) VALUES
-- 1: üres szabványos tábla, színválasztás még hátravan
('ures-6x7', 6, 7,
 '.......' ||
 '.......' ||
 '.......' ||
 '.......' ||
 '.......' ||
 '.......', NULL),
-- 2: a piros (te) egy lépésre van a győzelemtől: dobj a 4. oszlopba!
('piros-nyerhet', 6, 7,
 '.......' ||
 '.......' ||
 '.......' ||
 '.......' ||
 'YYY....' ||
 'RRR....', 'R'),
-- 3: a sárga függőleges hármast épített a 4. oszlopban – blokkolnod kell
('blokkolj', 6, 7,
 '.......' ||
 '.......' ||
 '.......' ||
 '...Y...' ||
 '...Y...' ||
 'RR.Y..R', 'R'),
-- 4: kisebb, 5×6-os tábla egy félbehagyott játszmával
('kicsi-5x6', 5, 6,
 '......' ||
 '......' ||
 '......' ||
 '..YR..' ||
 '.RYRY.', 'R');
