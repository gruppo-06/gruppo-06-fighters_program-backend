package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TurnNarratorTest {

    private final TurnNarrator narrator = new TurnNarrator();

    @Test
    void narratesBothStrikesOfARoundWithoutKnockout() {
        TurnLog turn = new TurnLog(1, 1,
                fighter("EA16", "Hana Kuroda", false, 281, 281, 281, 540, 240),
                fighter("EA10", "Darius Bell", false, 300, 300, 300, 680, 399),
                RoundOutcome.NO_KNOCKOUT, 0, 3000, 3000, null, null, "");

        assertEquals("""
                Hana Kuroda infligge 281 danni a Darius Bell. Darius Bell scende a 399 DEF.
                Darius Bell infligge 300 danni a Hana Kuroda. Hana Kuroda scende a 240 DEF.""",
                narrator.narrate(turn));
    }

    @Test
    void narratesFactionAdvantageKnockoutHealthDamageAndReplacement() {
        TurnLog turn = new TurnLog(2, 3,
                fighter("IP16", "Arjun Rao", true, 211, 222, 120, 300, 100),
                fighter("EA02", "Noah Bisset", false, 200, 200, 200, 120, 0),
                RoundOutcome.SECOND_KNOCKED_OUT, 102, 3000, 2898, null, new CardRef(1, "EA05", "Lena Hart"), "");

        assertEquals("""
                Arjun Rao ha il vantaggio di fazione: l'ATK sale da 211 a 222.
                Arjun Rao infligge 120 danni a Noah Bisset. Noah Bisset scende a 0 DEF.
                Noah Bisset infligge 200 danni a Arjun Rao. Arjun Rao scende a 100 DEF.
                Noah Bisset è KO.
                Il giocatore 2 perde 102 punti vita e scende a 2898.
                Il giocatore 2 schiera Lena Hart.""",
                narrator.narrate(turn));
    }

    @Test
    void narratesDoubleKnockout() {
        TurnLog turn = new TurnLog(1, 1,
                fighter("AA01", "Primo", false, 500, 500, 400, 400, 0),
                fighter("AA02", "Secondo", false, 500, 500, 400, 400, 0),
                RoundOutcome.DOUBLE_KNOCKOUT, 0, 1000, 1000, null, null, "");

        assertEquals("""
                Primo infligge 400 danni a Secondo. Secondo scende a 0 DEF.
                Secondo infligge 400 danni a Primo. Primo scende a 0 DEF.
                Doppio KO: nessun danno ai punti vita.""",
                narrator.narrate(turn));
    }

    private static FighterLog fighter(String code, String name, boolean advantage, int printedAttack,
            int effectiveAttack, int damageDealt, int defenseBefore, int defenseAfter) {
        return new FighterLog(0, code, name, Faction.EURO_ATLANTIC, advantage, printedAttack, effectiveAttack,
                damageDealt, defenseBefore, defenseAfter);
    }
}
