package it.fightersprogram.collection.domain;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.fightersprogram.collection.DeckRules;
import it.fightersprogram.collection.Violation;

public final class DeckRulebook {

    private static final String COMMON = "Comune";
    private static final String RARE = "Rara";
    private static final String EPIC = "Epica";
    private static final String LEGENDARY = "Leggendaria";
    private static final Map<String, Integer> SUBCARD_SLOTS =
            Map.of(COMMON, 3, RARE, 2, EPIC, 1, LEGENDARY, 0);
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
        deck.forEach(line -> subcardViolations(line, violations));
        return violations;
    }

    public static Violation violation(String code, Object... arguments) {
        return new Violation(code, MessageFormat.format(MESSAGES.getString(code), arguments));
    }

    private static int copiesOfRarity(List<DeckLine> deck, String rarity) {
        return deck.stream().filter(line -> rarity.equals(line.rarity())).mapToInt(DeckLine::copies).sum();
    }

    private static void subcardViolations(DeckLine line, List<Violation> violations) {
        Map<String, Long> counts = line.subcards().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        counts.forEach((code, count) -> {
            if (count > 1) {
                violations.add(violation("DUPLICATE_SUBCARD", line.cardName(), code));
            }
        });
        int slots = SUBCARD_SLOTS.getOrDefault(line.rarity(), 0);
        if (line.subcards().size() > slots) {
            violations.add(violation("TOO_MANY_SUBCARDS", line.cardName(), slots, line.rarity(),
                    line.subcards().size()));
        }
    }
}
