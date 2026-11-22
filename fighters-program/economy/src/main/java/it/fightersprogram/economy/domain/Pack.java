package it.fightersprogram.economy.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pack")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "pack_content", joinColumns = @jakarta.persistence.JoinColumn(name = "pack_id"))
    private List<PackContent> contents = new ArrayList<>();

    public Pack(String name, String description, int price, List<PackContent> contents) {
        if (price < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        if (contents == null || contents.isEmpty()) {
            throw new IllegalArgumentException("contents must not be empty");
        }
        this.name = name;
        this.description = description;
        this.price = price;
        this.contents = new ArrayList<>(contents);
    }

    public int totalCards() {
        return contents.stream().mapToInt(PackContent::getCardCount).sum();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void update(String name, String description, int price, List<PackContent> contents) {
        if (price < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        if (contents == null || contents.isEmpty()) {
            throw new IllegalArgumentException("contents must not be empty");
        }
        this.name = name;
        this.description = description;
        this.price = price;
        this.contents = new ArrayList<>(contents);
    }
}