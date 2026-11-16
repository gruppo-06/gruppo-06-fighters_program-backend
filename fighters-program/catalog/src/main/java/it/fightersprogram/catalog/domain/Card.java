package it.fightersprogram.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private String country;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faction_id")
    private Faction faction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id")
    private FighterClass fighterClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;

    private int atk;

    private int def;

    private int vel;

    private int crt;

    private String flavorText;

    private String imageFile;

    public Card(String code, CardDetails details) {
        this.code = code;
        update(details);
    }

    public void update(CardDetails details) {
        this.name = details.name();
        this.country = details.country();
        this.faction = details.faction();
        this.fighterClass = details.fighterClass();
        this.rarity = details.rarity();
        this.atk = details.atk();
        this.def = details.def();
        this.vel = details.vel();
        this.crt = details.crt();
        this.flavorText = details.flavorText();
        this.imageFile = details.imageFile();
    }
}
