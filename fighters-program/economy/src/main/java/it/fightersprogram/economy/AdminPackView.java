package it.fightersprogram.economy;

import java.util.List;

public record AdminPackView(long id, String name, String description, int price, boolean active, int totalCards,
        List<PackContentView> contents) {
}
