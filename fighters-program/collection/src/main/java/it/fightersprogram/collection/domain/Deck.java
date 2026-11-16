package it.fightersprogram.collection.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deck")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String name;

    private OffsetDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "deck_card", joinColumns = @JoinColumn(name = "deck_id"))
    private List<DeckCard> cards = new ArrayList<>();

    public Deck(long userId, String name, List<DeckCard> cards) {
        this.userId = userId;
        this.createdAt = OffsetDateTime.now();
        replace(name, cards);
    }

    public void replace(String newName, List<DeckCard> newCards) {
        this.name = newName;
        this.cards.clear();
        this.cards.addAll(newCards);
    }

    public int cardCount() {
        return cards.stream().mapToInt(DeckCard::getCopies).sum();
    }

    public List<DeckCard> getCards() {
        return List.copyOf(cards);
    }
}
