package it.fightersprogram.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CardTest {

    @Test
    void anImportedCardIsActiveAndNotManagedByAnAdmin() {
        Card card = new Card("ZT01", details("Imported", 100));

        assertTrue(card.isActive());
        assertFalse(card.isAdminManaged());
    }

    @Test
    void aCardCreatedByAnAdminIsActiveAndManagedByAnAdmin() {
        Card card = Card.createdByAdmin("ZT01", details("Created", 100));

        assertTrue(card.isActive());
        assertTrue(card.isAdminManaged());
        assertEquals("Created", card.getName());
    }

    @Test
    void anAdminEditBecomesManagedByAnAdmin() {
        Card card = new Card("ZT01", details("Imported", 100));

        card.editByAdmin(details("Edited", 200));

        assertTrue(card.isAdminManaged());
        assertEquals("Edited", card.getName());
        assertEquals(200, card.getAtk());
    }

    @Test
    void aCardCanBeDeactivatedAndActivatedAgain() {
        Card card = new Card("ZT01", details("Imported", 100));

        card.deactivate();
        assertFalse(card.isActive());

        card.activate();
        assertTrue(card.isActive());
    }

    private static CardDetails details(String name, int attack) {
        return new CardDetails(name, "Kenya", null, null, null, attack, 540, 850, 400, "Flavor", "ZT01.png");
    }
}
