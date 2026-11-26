# Architecture decisions

## 004 — Provisional cosmetic skin catalog

Until DSG_MO07 — Catalogo personalizzazioni avatar e variant (#132) is complete, the application ships a provisional
catalog of avatar skins and card variants. Its names, rarities, prices, and image paths are temporary data and can be
replaced with the catalog approved by the DSG. The catalog is loaded from JSON at startup and is keyed by skin code so
re-importing it updates existing entries instead of creating duplicates.
