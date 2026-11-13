CREATE TABLE faction (
    id   SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE fighter_class (
    id   SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE rarity (
    id   SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    rank SMALLINT NOT NULL UNIQUE
);

INSERT INTO faction (id, name) VALUES
    (1, 'Euroatlantica'),
    (2, 'Eurasiatica'),
    (3, 'Africana'),
    (4, 'Panamericana'),
    (5, 'Indo-Pacifica');

INSERT INTO fighter_class (id, name) VALUES
    (1, 'Combattenti'),
    (2, 'Assassini'),
    (3, 'Specialisti'),
    (4, 'Colossi'),
    (5, 'Sentinelle');

INSERT INTO rarity (id, name, rank) VALUES
    (1, 'Comune', 1),
    (2, 'Rara', 2),
    (3, 'Epica', 3),
    (4, 'Leggendaria', 4);
