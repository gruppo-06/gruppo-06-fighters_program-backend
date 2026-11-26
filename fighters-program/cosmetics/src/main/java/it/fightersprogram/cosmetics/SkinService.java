package it.fightersprogram.cosmetics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.cosmetics.domain.UserSkin;
import it.fightersprogram.cosmetics.domain.UserSkinId;
import it.fightersprogram.cosmetics.domain.UserSkinRepository;
import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;
import it.fightersprogram.cosmetics.domain.Skin;
import it.fightersprogram.cosmetics.domain.SkinRepository;
import it.fightersprogram.cosmetics.domain.SkinType;
import it.fightersprogram.identity.UserDirectory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkinService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkinService.class);

    private final SkinRepository skinRepository;
    private final UserSkinRepository userSkinRepository;
    private final CatalogService catalogService;
    private final CreditService creditService;
    private final UserDirectory userDirectory;

    public Page<SkinView> findSkins(SkinType type, String cardCode, Pageable pageable) {
        Long cardId = resolveCardId(cardCode);
        if (cardCode != null && cardId == null) {
            return Page.empty(pageable);
        }
        Specification<Skin> specification = (root, query, builder) -> builder.isTrue(root.get("active"));
        if (type != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("type"), type));
        }
        if (cardId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("cardId"), cardId));
        }
        return toViews(skinRepository.findAll(specification, pageable));
    }

    public SkinView findByCode(String code) {
        Skin skin = skinRepository.findByCode(code)
                .filter(Skin::isActive)
                .orElseThrow(() -> new NotFoundException("Skin not found: " + code));
        return toViews(List.of(skin)).getFirst();
    }

    @Transactional
    public SkinView purchase(long userId, long skinId) {
        Skin skin = activeSkin(skinId);
        if (userSkinRepository.existsById(new UserSkinId(userId, skinId))) {
            throw new ConflictException("Skin is already owned: " + skinId);
        }
        if (skin.getPrice() > 0) {
            creditService.debit(userId, skin.getPrice(), CreditType.SKIN_PURCHASE,
                    "skin:" + userId + ":" + skinId);
        }
        grant(userId, skinId, "PURCHASE");
        return toViews(List.of(skin)).getFirst();
    }

    @Transactional
    public void grant(long userId, long skinId, String reason) {
        skinRepository.findById(skinId)
                .orElseThrow(() -> new NotFoundException("Skin not found: " + skinId));
        userDirectory.nicknameOf(userId);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required when granting a skin");
        }
        if (userSkinRepository.insertIfAbsent(userId, skinId) == 1) {
            LOGGER.info("Granted skin {} to user {} ({})", skinId, userId, reason);
        }
    }

    @Transactional(readOnly = true)
    public List<OwnedSkinView> findOwnedSkins(long userId) {
        List<UserSkin> ownedSkins = userSkinRepository.findAllByUserIdOrderByAcquiredAtDescSkinIdAsc(userId);
        Map<Long, SkinView> viewsById = toViews(skinRepository.findAllById(
                ownedSkins.stream().map(UserSkin::getSkinId).toList())).stream()
                .collect(Collectors.toMap(SkinView::id, Function.identity()));
        return ownedSkins.stream()
                .map(owned -> new OwnedSkinView(viewsById.get(owned.getSkinId()), owned.getAcquiredAt()))
                .toList();
    }

    private Skin activeSkin(long skinId) {
        return skinRepository.findById(skinId)
                .filter(Skin::isActive)
                .orElseThrow(() -> new NotFoundException("Skin not found: " + skinId));
    }

    private Long resolveCardId(String cardCode) {
        if (cardCode == null) {
            return null;
        }
        return catalogService.findCardsByCodes(List.of(cardCode)).stream()
                .findFirst()
                .map(card -> card.id())
                .orElse(null);
    }

    private Page<SkinView> toViews(Page<Skin> skins) {
        Map<Long, SkinView> viewsById = toViews(skins.getContent()).stream()
                .collect(Collectors.toMap(SkinView::id, Function.identity()));
        return skins.map(skin -> viewsById.get(skin.getId()));
    }

    private List<SkinView> toViews(Collection<Skin> skins) {
        List<Long> cardIds = skins.stream()
                .map(Skin::getCardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> cardCodes = cardIds.isEmpty() ? Map.of() : catalogService.findCardsByIds(cardIds).stream()
                .collect(Collectors.toMap(card -> card.id(), card -> card.code()));
        Map<Short, String> rarities = catalogService.rarityNamesById();
        return skins.stream()
                .map(skin -> new SkinView(skin.getId(), skin.getCode(), skin.getName(), skin.getType(),
                        skin.getCardId() == null ? null : cardCodes.get(skin.getCardId()),
                        rarities.get(skin.getRarityId()), skin.getPrice(), skin.getImageFile()))
                .toList();
    }
}
