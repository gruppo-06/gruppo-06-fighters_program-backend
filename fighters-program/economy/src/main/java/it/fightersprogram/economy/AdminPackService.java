package it.fightersprogram.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.common.error.UnprocessableException;
import it.fightersprogram.economy.domain.Pack;
import it.fightersprogram.economy.domain.PackContent;
import it.fightersprogram.economy.domain.PackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPackService {

    private final PackRepository packRepository;
    private final CatalogService catalogService;

    public List<AdminPackView> allPacks() {
        Map<Short, String> rarityNames = catalogService.rarityNamesById();
        return packRepository.findAll(Sort.by("price", "id")).stream()
                .map(pack -> toView(pack, rarityNames))
                .toList();
    }

    public AdminPackView create(AdminPackRequest request) {
        List<PackContent> contents = toContents(request.contents());
        requireUniqueName(request.name(), null);
        Pack pack = new Pack(request.name(), request.description(), request.price(), contents);
        if (Boolean.FALSE.equals(request.active())) {
            pack.deactivate();
        }
        return toView(packRepository.save(pack), catalogService.rarityNamesById());
    }

    public AdminPackView edit(long id, AdminPackRequest request) {
        Pack pack = findPack(id);
        List<PackContent> contents = toContents(request.contents());
        requireUniqueName(request.name(), id);
        pack.update(request.name(), request.description(), request.price(), contents);
        if (Boolean.TRUE.equals(request.active())) {
            pack.activate();
        } else if (Boolean.FALSE.equals(request.active())) {
            pack.deactivate();
        }
        return toView(pack, catalogService.rarityNamesById());
    }

    public void deactivate(long id) {
        findPack(id).deactivate();
    }

    private Pack findPack(long id) {
        return packRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pack not found: " + id));
    }

    private void requireUniqueName(String name, Long ownId) {
        packRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ConflictException("A pack named " + name + " already exists");
                });
    }

    private List<PackContent> toContents(List<AdminPackContentRequest> requests) {
        Set<String> available = catalogService.findAllCards().stream()
                .map(CatalogCard::rarity)
                .collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        List<PackContent> contents = new ArrayList<>();
        for (AdminPackContentRequest request : requests) {
            if (!seen.add(request.rarity())) {
                throw new BadRequestException("The rarity " + request.rarity() + " appears more than once in the pack");
            }
            short rarityId = catalogService.rarityIdByName(request.rarity())
                    .orElseThrow(() -> new UnprocessableException("UNKNOWN_RARITY",
                            "Unknown rarity: " + request.rarity(), List.of("rarity: " + request.rarity())));
            if (!available.contains(request.rarity())) {
                throw new UnprocessableException("PACK_UNAVAILABLE",
                        "The pack cannot be opened: no active " + request.rarity() + " cards are available", List.of());
            }
            contents.add(new PackContent(rarityId, request.cardCount()));
        }
        return contents;
    }

    private static AdminPackView toView(Pack pack, Map<Short, String> rarityNames) {
        List<PackContentView> contents = pack.getContents().stream()
                .sorted(Comparator.comparing(PackContent::getRarityId))
                .map(content -> new PackContentView(
                        rarityNames.getOrDefault(content.getRarityId(), "Unknown"),
                        content.getCardCount()))
                .toList();
        return new AdminPackView(pack.getId(), pack.getName(), pack.getDescription(), pack.getPrice(),
                pack.isActive(), pack.totalCards(), contents);
    }
}
