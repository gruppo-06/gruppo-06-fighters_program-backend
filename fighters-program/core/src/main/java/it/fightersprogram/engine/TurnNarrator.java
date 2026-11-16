package it.fightersprogram.engine;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Turns a logged round into sentences ready to show to the player. Kept apart from the combat logic so the wording
 * can change without touching the rules.
 */
public final class TurnNarrator {

    private static final String BUNDLE = "it.fightersprogram.engine.narration";
    private static final Locale LOCALE = Locale.ITALIAN;
    private static final int PLAYER_ONE = 1;
    private static final int PLAYER_TWO = 2;

    private final ResourceBundle messages = ResourceBundle.getBundle(BUNDLE, Locale.ROOT);

    public String narrate(TurnLog turn) {
        List<String> lines = new ArrayList<>();
        addFactionAdvantage(lines, turn.fighterOne());
        addFactionAdvantage(lines, turn.fighterTwo());
        lines.add(strike(turn.fighterOne(), turn.fighterTwo()));
        lines.add(strike(turn.fighterTwo(), turn.fighterOne()));
        addKnockout(lines, turn);
        addReplacement(lines, PLAYER_ONE, turn.replacementOne());
        addReplacement(lines, PLAYER_TWO, turn.replacementTwo());
        return String.join("\n", lines);
    }

    private void addFactionAdvantage(List<String> lines, FighterLog fighter) {
        if (fighter.hasFactionAdvantage()) {
            lines.add(format("factionAdvantage", fighter.cardName(), fighter.printedAttack(),
                    fighter.effectiveAttack()));
        }
    }

    private String strike(FighterLog attacker, FighterLog defender) {
        return format("strike", attacker.cardName(), attacker.damageDealt(), defender.cardName(),
                defender.defenseAfter());
    }

    private void addKnockout(List<String> lines, TurnLog turn) {
        switch (turn.outcome()) {
            case FIRST_KNOCKED_OUT -> addSingleKnockout(lines, turn.fighterOne(), PLAYER_ONE, turn.healthDamage(),
                    turn.healthOneAfter());
            case SECOND_KNOCKED_OUT -> addSingleKnockout(lines, turn.fighterTwo(), PLAYER_TWO, turn.healthDamage(),
                    turn.healthTwoAfter());
            case DOUBLE_KNOCKOUT -> lines.add(format("doubleKnockout"));
            case NO_KNOCKOUT -> {
            }
        }
    }

    private void addSingleKnockout(List<String> lines, FighterLog loser, int player, int healthDamage,
            int healthAfter) {
        lines.add(format("knockout", loser.cardName()));
        if (healthDamage > 0) {
            lines.add(format("healthDamage", player, healthDamage, healthAfter));
        }
    }

    private void addReplacement(List<String> lines, int player, CardRef replacement) {
        if (replacement != null) {
            lines.add(format("replacement", player, replacement.name()));
        }
    }

    private String format(String key, Object... arguments) {
        return new MessageFormat(messages.getString(key), LOCALE).format(arguments);
    }
}
