package it.fightersprogram.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.domain.Card;
import it.fightersprogram.catalog.domain.CardRepository;
import it.fightersprogram.catalog.domain.CardSpecifications;
import it.fightersprogram.common.error.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CardRepository cardRepository;

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
