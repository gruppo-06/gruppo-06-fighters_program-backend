package it.fightersprogram.economy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.economy.domain.Pack;
import it.fightersprogram.economy.domain.PackContent;
import it.fightersprogram.economy.domain.PackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackService {

    private final PackRepository packRepository;
    private final CatalogService catalogService;

    public List<PackView> activePacks() {
        Map<Short, String> rarityNames = catalogService.rarityNamesById();
        return packRepository.findAllByActiveTrueOrderByPriceAsc().stream()
                .map(pack -> toPackView(pack, rarityNames))
                .toList();
    }

    public PackView activePack(long id) {
        Map<Short, String> rarityNames = catalogService.rarityNamesById();
        Pack pack = packRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Pack not found: " + id));
        return toPackView(pack, rarityNames);
    }

    private PackView toPackView(Pack pack, Map<Short, String> rarityNames) {
        List<PackContentView> contents = pack.getContents().stream()
                .sorted(Comparator.comparing(PackContent::getRarityId))
                .map(content -> new PackContentView(
                        rarityNames.getOrDefault(content.getRarityId(), "Unknown"),
                        content.getCardCount()))
                .toList();
        return new PackView(pack.getId(), pack.getName(), pack.getDescription(), pack.getPrice(),
                pack.totalCards(), contents);
    }
}