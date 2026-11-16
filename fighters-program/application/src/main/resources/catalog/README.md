# Card catalog

The 80 Fighters of the DSG delivery of 9 November: 16 per faction, 8 Comune / 4 Rara / 3 Epica / 1 Leggendaria.

| File | Source |
|---|---|
| `catalogo-euroatlantica.json` | Official DSG file (`03_Carte/Carte-Finali/Euroatlantica` on the Drive), unchanged |
| the other four | Generated from the DSG roster `03_Carte/Catalogo/Roaster/Catalogo fighters.xlsx`, rows with *Prima consegna = Sì* |

The four generated files follow the official file field by field: same schema, `copy` from the *Copy carta* column,
image named `<code with three digits>_<class>_<rarity>_<name>.webp`. Regenerating the Euroatlantica file the same way
gives exactly the official one.

When the DSG deliver their own files for the other factions, replace them: the startup import updates the cards by
code, so no migration is needed. The Africana card files on the Drive use `_` instead of spaces in their names; the
JSON keeps spaces, like the official file.
