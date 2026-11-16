package it.fightersprogram.collection.domain;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import it.fightersprogram.collection.DeckRules;
import it.fightersprogram.collection.Violation;

public final class DeckRulebook {

    private static final String EPIC = "Epica";
    private static final String LEGENDARY = "Leggendaria";
    private static final ResourceBundle MESSAGES =
            ResourceBundle.getBundle("it.fightersprogram.collection.deck-violations", Locale.ITALIAN);

    private final DeckRules rules;

    public DeckRulebook(DeckRules rules) {
        this.rules = rules;
    }

    public List<Violation> check(List<DeckLine> deck) {
        List<Violation> violations = new ArrayList<>();
        int size = deck.stream().mapToInt(DeckLine::copies).sum();
        if (size != rules.size()) {
            violations.add(violation("WRONG_SIZE", rules.size(), size));
        }
        deck.stream()
                .filter(line -> line.copies() > rules.maxCopies())
                .forEach(line -> violations.add(violation("TOO_MANY_COPIES", rules.maxCopies(), line.cardName())));
        int epic = copiesOfRarity(deck, EPIC);
        if (epic > rules.maxEpic()) {
            violations.add(violation("TOO_MANY_EPIC", rules.maxEpic(), epic));
        }
        int legendary = copiesOfRarity(deck, LEGENDARY);
        if (legendary > rules.maxLegendary()) {
            violations.add(violation("TOO_MANY_LEGENDARY", rules.maxLegendary(), legendary));
        }
        int power = deck.stream().mapToInt(line -> rules.pointsOf(line.rarity()) * line.copies()).sum();
        if (power > rules.powerBudget()) {
            violations.add(violation("OVER_BUDGET", rules.powerBudget(), power));
        }
        deck.stream()
                .filter(line -> line.copies() > line.ownedCopies())
                .forEach(line -> violations.add(
                        violation("NOT_OWNED", line.copies() - line.ownedCopies(), line.cardName())));
        return violations;
    }

    public static Violation violation(String code, Object... arguments) {
        return new Violation(code, MessageFormat.format(MESSAGES.getString(code), arguments));
    }

    private static int copiesOfRarity(List<DeckLine> deck, String rarity) {
        return deck.stream().filter(line -> rarity.equals(line.rarity())).mapToInt(DeckLine::copies).sum();
    }
}
