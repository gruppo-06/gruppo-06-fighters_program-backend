# Card catalog

The 80 Fighters of the **"proposta di carte"** delivery of 22 November 2026: 16 per faction, 8 Comune / 4 Rara /
3 Epica / 1 Leggendaria. It replaces the roster of the DSG delivery of 9 November.

| Field | Source | Status |
|---|---|---|
| `name`, `country` | Faction documents (`Confederazione <faction>.pdf`): name and *Nazionalità* | Delivered |
| `class` | Faction documents: section (Combattenti, Assassini, Specialisti, Colossi, Sentinelle) | Delivered |
| `id` | Faction prefix + the character's number in its document (`AF01`–`AF16`, `EU`, `EA`, `IP`, `PA`) | Derived |
| Leggendaria | Faction documents and illustration names (`(Leggendaria)`) | Delivered |
| Other rarities | Per faction, Epica and then Rara assigned in turn to different classes, in document order; the rest Comune | **Provisional** |
| `stats` | Taken from the previous catalog's cards of the same class and rarity, which the DSG had balanced | **Provisional** |
| `copy` | A sentence of the character's story in the faction document | **Provisional** |
| `image` | `images/<id>.webp`: the delivered PNG illustration converted to WebP 900x1200 | Delivered |

When the DSG confirm rarities, stats or copy, edit the JSON files: the startup import updates cards by code, so no
migration is needed (cards an admin created or edited are left alone). The rarity distribution per faction is checked
by `CatalogValidator`.
