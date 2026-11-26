package it.fightersprogram.cosmetics.importer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.cosmetics.domain.Skin;
import it.fightersprogram.cosmetics.domain.SkinRepository;
import it.fightersprogram.cosmetics.domain.SkinType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkinImporter {

    private final SkinRepository skinRepository;
    private final CatalogService catalogService;

    @Transactional
    public void importAll(List<SkinEntry> entries) {
        List<String> cardCodes = entries.stream()
                .filter(entry -> SkinType.CARD_VARIANT.name().equals(entry.type()))
                .map(SkinEntry::cardCode)
                .filter(code -> code != null)
                .distinct()
                .toList();
        Map<String, CatalogCard> cardsByCode = catalogService.findCardsByCodes(cardCodes).stream()
                .collect(Collectors.toMap(CatalogCard::code, Function.identity()));
        Map<String, Short> rarityIds = catalogService.rarityNamesById().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        List<ResolvedSkin> resolved = entries.stream().map(entry -> resolve(entry, cardsByCode, rarityIds)).toList();
        resolved.forEach(this::upsert);
    }

    private ResolvedSkin resolve(SkinEntry entry, Map<String, CatalogCard> cardsByCode,
            Map<String, Short> rarityIds) {
        if (entry.code() == null || entry.code().isBlank() || entry.code().length() > 20) {
            throw new SkinImportException("Skin has an invalid code: " + entry.code());
        }
        if (entry.name() == null || entry.name().isBlank()) {
            throw new SkinImportException("Skin " + entry.code() + " has no name");
        }
        if (entry.imageFile() == null || entry.imageFile().isBlank()) {
            throw new SkinImportException("Skin " + entry.code() + " has no imageFile");
        }
        if (entry.price() < 0) {
            throw new SkinImportException("Skin " + entry.code() + " has a negative price");
        }

        SkinType type;
        try {
            type = SkinType.valueOf(entry.type());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SkinImportException("Skin " + entry.code() + " has unknown type '" + entry.type() + "'");
        }

        Long cardId = null;
        if (type == SkinType.CARD_VARIANT) {
            CatalogCard card = cardsByCode.get(entry.cardCode());
            if (card == null) {
                throw new SkinImportException("Skin " + entry.code() + " references unknown cardCode '"
                        + entry.cardCode() + "'");
            }
            cardId = card.id();
        } else if (entry.cardCode() != null) {
            throw new SkinImportException("Avatar skin " + entry.code() + " must not define cardCode");
        }

        Short rarityId = rarityIds.get(entry.rarity());
        if (rarityId == null) {
            throw new SkinImportException("Skin " + entry.code() + " references unknown rarity '"
                    + entry.rarity() + "'");
        }
        return new ResolvedSkin(entry, type, cardId, rarityId);
    }

    private void upsert(ResolvedSkin resolved) {
        SkinEntry entry = resolved.entry();
        skinRepository.findByCode(entry.code())
                .ifPresentOrElse(
                        existing -> existing.update(entry.name(), resolved.type(), resolved.cardId(),
                                resolved.rarityId(), entry.price(), entry.imageFile()),
                        () -> skinRepository.save(new Skin(entry.code(), entry.name(), resolved.type(),
                                resolved.cardId(), resolved.rarityId(), entry.price(), entry.imageFile())));
    }

    private record ResolvedSkin(SkinEntry entry, SkinType type, Long cardId, Short rarityId) {
    }
}
