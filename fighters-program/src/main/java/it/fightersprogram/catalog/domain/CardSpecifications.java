package it.fightersprogram.catalog.domain;

import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> hasFaction(String name) {
        return (root, query, builder) -> name == null ? null : builder.equal(root.get("faction").get("name"), name);
    }

    public static Specification<Card> hasClass(String name) {
        return (root, query, builder) -> name == null ? null : builder.equal(root.get("fighterClass").get("name"), name);
    }

    public static Specification<Card> hasRarity(String name) {
        return (root, query, builder) -> name == null ? null : builder.equal(root.get("rarity").get("name"), name);
    }
}
