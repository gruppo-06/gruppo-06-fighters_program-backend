# Base-mode rules: open values (SWD_EN01, #11)

Values the engine and the deck validator need. **Status "Provisional"** means the backend picked a value so the
game is playable; the DSG still have to confirm or replace it (#23, #24). All values live in one place:
`fighters-program/application/src/main/resources/application.properties` (`game.rules.*`, `game.deck.*`).

DSG sources on the Drive (`02_Game-Design`): *Regolamento di gioco*, *Meccaniche di gioco* and *Bilanciamento di gioco*.
They fix the base-mode mechanics (simultaneous strikes, residual DEF kept, +5% ATK for the favoured faction, health
damage as the survivor's attack minus the loser's DEF before the hit) and list as still open: starting health, deck
size and constraints, turn limit, outcome when a deck runs out or the limit is reached, and how to round the +5%.

| Value | Property | Provisional value | Status | Rationale |
|---|---|---|---|---|
| Starting health | `game.rules.starting-health` | 500 | Provisional | 2000 simulated matches on the 80 delivered Fighters (1 Leg, 2 Epic, 3 Rare, 4 Common per deck): 9.5 turns on average, 92% end by health, no draws |
| Deck size | `game.deck.size` | 10 | Provisional | The DSG documents mention 95 cards per player including the 15 Effect cards of the advanced mode, composition still open; the delivered catalog has 80 Fighters, so 10 keeps a match short and readable in the replay |
| Max copies of one card | `game.deck.max-copies` | 2 | Provisional | Variety in the deck while still allowing a favourite card twice |
| Max Epic cards | `game.deck.max-epic` | 2 | Provisional | Only 3 Epic per faction exist |
| Max Legendary cards | `game.deck.max-legendary` | 1 | Provisional | Only 1 Legendary per faction exists |
| Power budget | `game.deck.power-budget` | 18 (Comune 1, Rara 2, Epica 3, Leggendaria 5) | Provisional | Blocks a deck of only strong cards; 1 Leg + 2 Epic + 3 Rare + 4 Common = 21 is rejected, 1 Leg + 1 Epic + 2 Rare + 6 Common = 18 is accepted |
| Faction bonus | `game.rules.faction-bonus-percent` | 5 | Provisional (DSG) | The DSG documents call the +5% a playtest hypothesis |
| Bonus rounding | engine (`RoundResolver`) | Rounded half up on every hit (211 → 222) | Provisional | The DSG balance analysis keeps the decimals; with them IP16 vs EA02 ends in a double KO, with rounding Arjun wins in round 3. Four pairs of the catalog change outcome: the DSG must pick one |
| Health damage | engine (`MatchEngine`) | Attack with bonus minus the loser's defense before the lethal hit | Provisional | Same convention as the DSG worked examples |
| Turn limit | `game.rules.max-turns` | 20 | Provisional | A turn ends at a knockout, so a 10-card match has at most 19 turns: the limit is a safety net |
| Outcome at the turn limit | engine | Healthier player wins, equal health is a draw | Provisional | |
| Outcome when a deck runs out | engine | The player who cannot replace a knocked-out card loses; both at once: decided by health | Provisional | |
| Max decks per user | `game.deck.max-per-user` | 10 | Decided | From #57 |

## Economy: open values (SWD_EC01 #13, SWD_EC02 #9)

Credit values live in `application.properties` (`game.credits.*`); pack prices and contents in
`V9__seed_packs.sql` and, from then on, in the admin endpoints (`/api/admin/packs`). The DSG still have to confirm
them with DSG_MO01, DSG_MO02 and DSG_MO03.

| Value | Property | Provisional value | Status | Rationale |
|---|---|---|---|---|
| Welcome bonus | `game.credits.welcome` | 500 | Provisional | Five Base packs, or one Leggendario |
| Match win | `game.credits.match-win` | 50 | Provisional | A draw gives nothing |
| Daily streak | `game.credits.daily-streak` | 10, 15, 20, 25, 30, 40, 50 | Provisional | Day 8 and later stay at 50; the game day is Europe/Rome |
| Pacchetto Base | `V9__seed_packs.sql` | 100 credits: 4 Comune, 1 Rara | Provisional | Prices taken from the frontend mock of the Negozio |
| Pacchetto Avanzato | `V9__seed_packs.sql` | 220 credits: 5 Comune, 2 Rara, 1 Epica | Provisional | |
| Pacchetto Leggendario | `V9__seed_packs.sql` | 500 credits: 6 Comune, 2 Rara, 1 Epica, 1 Leggendaria | Provisional | |
| Draw within a rarity | `PackOpeningService` | Uniform, with replacement, among the active cards of the rarity | Provisional | Effect cards are not in packs yet (advanced mode) |

## Card roster: open values (#185)

Since 22 November 2026 the catalog is the **"proposta di carte"** roster (see
`fighters-program/application/src/main/resources/catalog/README.md`). Its documents give names, nationalities, classes
and the legendary of each faction; everything else is provisional.

| Value | Where | Provisional value | Status | Rationale |
|---|---|---|---|---|
| Rarity of non-legendary cards | `catalogo-*.json` | 8 Comune, 4 Rara, 3 Epica per faction; Epica and Rara spread over different classes in document order | Provisional (DSG) | Same distribution as the previous delivery, which the deck rules and packs assume |
| Card stats | `catalogo-*.json` | Values of the previous catalog's cards with the same class and rarity | Provisional (DSG) | Keeps the balance the DSG tuned per class and rarity |
| Card copy | `catalogo-*.json` | A sentence of the character's story | Provisional (DSG) | |
||||||| cdfe471
