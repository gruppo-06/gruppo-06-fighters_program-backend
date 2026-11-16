# Base-mode rules: open values (SWD_EN01, #11)

Values the engine and the deck validator need. **Status "Provisional"** means the backend picked a value so the
game is playable; the DSG still have to confirm or replace it (#23, #24). All values live in one place:
`fighters-program/application/src/main/resources/application.properties` (`game.rules.*`, `game.deck.*`).

| Value | Property | Provisional value | Status | Rationale |
|---|---|---|---|---|
| Starting health | `game.rules.starting-health` | 500 | Provisional | 2000 simulated matches on the provisional catalogs: 10.5 turns on average, 88% end by health, almost no draws |
| Deck size | `game.deck.size` | 10 | Provisional | The documents mention 95 cards but the catalog has 80 Fighters; 10 keeps a match short and readable in the replay |
| Max copies of one card | `game.deck.max-copies` | 2 | Provisional | Variety in the deck while still allowing a favourite card twice |
| Max Epic cards | `game.deck.max-epic` | 2 | Provisional | Only 3 Epic per faction exist |
| Max Legendary cards | `game.deck.max-legendary` | 1 | Provisional | Only 1 Legendary per faction exists |
| Power budget | `game.deck.power-budget` | 18 (Comune 1, Rara 2, Epica 3, Leggendaria 5) | Provisional | Blocks a deck of only strong cards; 1 Leg + 2 Epic + 3 Rare + 4 Common = 21 is rejected, 1 Leg + 1 Epic + 2 Rare + 6 Common = 18 is accepted |
| Faction bonus | `game.rules.faction-bonus-percent` | 5 | Decided | From the rules document |
| Bonus rounding | engine (`RoundResolver`) | Rounded half up on every hit (211 → 222) | Provisional | The balance document shows 4 pairs that change outcome: the DSG must pick one |
| Health damage | engine (`MatchEngine`) | Attack with bonus minus the loser's defense before the lethal hit | Provisional | Same convention as the DSG worked examples |
| Turn limit | `game.rules.max-turns` | 20 | Provisional | A turn ends at a knockout, so a 10-card match has at most 19 turns: the limit is a safety net |
| Outcome at the turn limit | engine | Healthier player wins, equal health is a draw | Provisional | |
| Outcome when a deck runs out | engine | The player who cannot replace a knocked-out card loses; both at once: decided by health | Provisional | |
| Max decks per user | `game.deck.max-per-user` | 10 | Decided | From #57 |
