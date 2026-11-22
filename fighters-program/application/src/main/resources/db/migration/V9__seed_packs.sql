-- PROVISIONAL seed values for development
INSERT INTO pack (name, description, price) VALUES
    ('Pacchetto Base', '5 carte: 4 comuni, 1 rara', 100),
    ('Pacchetto Avanzato', '8 carte: 5 comuni, 2 rare, 1 epica', 220),
    ('Pacchetto Leggendario', '10 carte: 1 leggendaria garantita', 500);

INSERT INTO pack_content (pack_id, rarity_id, card_count) VALUES
    ((SELECT id FROM pack WHERE name = 'Pacchetto Base'), (SELECT id FROM rarity WHERE name = 'Comune'), 4),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Base'), (SELECT id FROM rarity WHERE name = 'Rara'), 1),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Avanzato'), (SELECT id FROM rarity WHERE name = 'Comune'), 5),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Avanzato'), (SELECT id FROM rarity WHERE name = 'Rara'), 2),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Avanzato'), (SELECT id FROM rarity WHERE name = 'Epica'), 1),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Leggendario'), (SELECT id FROM rarity WHERE name = 'Comune'), 6),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Leggendario'), (SELECT id FROM rarity WHERE name = 'Rara'), 2),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Leggendario'), (SELECT id FROM rarity WHERE name = 'Epica'), 1),
    ((SELECT id FROM pack WHERE name = 'Pacchetto Leggendario'), (SELECT id FROM rarity WHERE name = 'Leggendaria'), 1);