package it.fightersprogram.economy;

import java.util.List;

public record PackView(long id, String name, String description, int price, int totalCards, List<PackContentView> contents) {
}