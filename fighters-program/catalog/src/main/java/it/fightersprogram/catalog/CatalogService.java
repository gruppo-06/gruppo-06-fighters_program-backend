package it.fightersprogram.catalog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.domain.Card;
import it.fightersprogram.catalog.domain.CardRepository;
import it.fightersprogram.catalog.domain.CardSpecifications;
import it.fightersprogram.catalog.domain.Faction;
import it.fightersprogram.catalog.domain.FactionRepository;
import it.fightersprogram.common.error.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CardRepository cardRepository;
    private final FactionRepository factionRepository;

    public Page<CardResponse> findCards(CardFilter filter, Pageable pageable) {
        Specification<Card> specification = Specification.allOf(
                CardSpecifications.hasFaction(filter.faction()),
                CardSpecifications.hasClass(filter.fighterClass()),
                CardSpecifications.hasRarity(filter.rarity()));
        return cardRepository.findAll(specification, pageable).map(this::toResponse);
    }

    public CardResponse findByCode(String code) {
        return cardRepository.findByCode(code)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Card not found: " + code));
    }

    public List<CatalogCard> findAllCards() {
        return cardRepository.findAllByOrderByCodeAsc().stream().map(CatalogService::toCatalogCard).toList();
    }

    public List<CatalogCard> findCardsByIds(Collection<Long> ids) {
        return cardRepository.findAllByIdIn(ids).stream().map(CatalogService::toCatalogCard).toList();
    }

    public List<String> factionNames() {
        return factionRepository.findAll(Sort.by("id")).stream().map(Faction::getName).toList();
    }

    private static CatalogCard toCatalogCard(Card card) {
        return new CatalogCard(card.getId(), card.getCode(), card.getName(), card.getFaction().getName(),
                card.getFighterClass().getName(), card.getRarity().getName(), card.getAtk(), card.getDef(),
                card.getVel(), card.getCrt(), card.getImageFile());
    }

    private CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getCode(),
                card.getName(),
                card.getCountry(),
                card.getFaction().getName(),
                card.getFighterClass().getName(),
                card.getRarity().getName(),
                card.getAtk(),
                card.getDef(),
                card.getVel(),
                card.getCrt(),
                card.getFlavorText(),
                card.getImageFile());
    }
}
