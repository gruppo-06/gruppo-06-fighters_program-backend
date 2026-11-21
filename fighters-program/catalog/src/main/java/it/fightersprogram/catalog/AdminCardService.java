package it.fightersprogram.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.domain.Card;
import it.fightersprogram.catalog.domain.CardDetails;
import it.fightersprogram.catalog.domain.CardRepository;
import it.fightersprogram.catalog.domain.Faction;
import it.fightersprogram.catalog.domain.FactionRepository;
import it.fightersprogram.catalog.domain.FighterClass;
import it.fightersprogram.catalog.domain.FighterClassRepository;
import it.fightersprogram.catalog.domain.Rarity;
import it.fightersprogram.catalog.domain.RarityRepository;
import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.common.error.UnprocessableException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCardService {

    private final CardRepository cardRepository;
    private final FactionRepository factionRepository;
    private final FighterClassRepository fighterClassRepository;
    private final RarityRepository rarityRepository;

    public AdminCardResponse create(AdminCardRequest request) {
        if (cardRepository.findByCode(request.code()).isPresent()) {
            throw new ConflictException("A card with code " + request.code() + " already exists");
        }
        Card card = Card.createdByAdmin(request.code(), toDetails(request));
        return toResponse(cardRepository.save(card));
    }

    public AdminCardResponse edit(String code, AdminCardRequest request) {
        if (!code.equals(request.code())) {
            throw new BadRequestException("The code of a card cannot be changed");
        }
        Card card = findCard(code);
        card.editByAdmin(toDetails(request));
        if (Boolean.TRUE.equals(request.active())) {
            card.activate();
        } else if (Boolean.FALSE.equals(request.active())) {
            card.deactivate();
        }
        return toResponse(card);
    }

    public void deactivate(String code) {
        findCard(code).deactivate();
    }

    private Card findCard(String code) {
        return cardRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Card not found: " + code));
    }

    private CardDetails toDetails(AdminCardRequest request) {
        Faction faction = factionRepository.findByName(request.faction())
                .orElseThrow(() -> unknownReference("faction", request.faction()));
        FighterClass fighterClass = fighterClassRepository.findByName(request.fighterClass())
                .orElseThrow(() -> unknownReference("fighterClass", request.fighterClass()));
        Rarity rarity = rarityRepository.findByName(request.rarity())
                .orElseThrow(() -> unknownReference("rarity", request.rarity()));
        return new CardDetails(request.name(), request.country(), faction, fighterClass, rarity, request.atk(),
                request.def(), request.vel(), request.crt(), request.flavorText(), request.imageFile());
    }

    private static UnprocessableException unknownReference(String field, String value) {
        return new UnprocessableException("UNKNOWN_REFERENCE", "Unknown " + field + " '" + value + "'",
                List.of(field + ": " + value));
    }

    private static AdminCardResponse toResponse(Card card) {
        return new AdminCardResponse(card.getCode(), card.getName(), card.getCountry(), card.getFaction().getName(),
                card.getFighterClass().getName(), card.getRarity().getName(), card.getAtk(), card.getDef(),
                card.getVel(), card.getCrt(), card.getFlavorText(), card.getImageFile(), card.isActive());
    }
}
