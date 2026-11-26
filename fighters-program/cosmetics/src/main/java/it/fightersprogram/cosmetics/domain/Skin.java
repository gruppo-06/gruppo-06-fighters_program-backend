package it.fightersprogram.cosmetics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "skin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Skin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SkinType type;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "rarity_id", nullable = false)
    private Short rarityId;

    @Column(nullable = false)
    private int price;

    @Column(name = "image_file", nullable = false)
    private String imageFile;

    @Column(nullable = false)
    private boolean active = true;

    public Skin(String code, String name, SkinType type, Long cardId, Short rarityId, int price, String imageFile) {
        this.code = code;
        update(name, type, cardId, rarityId, price, imageFile);
    }

    public void update(String name, SkinType type, Long cardId, Short rarityId, int price, String imageFile) {
        this.name = name;
        this.type = type;
        this.cardId = cardId;
        this.rarityId = rarityId;
        this.price = price;
        this.imageFile = imageFile;
    }
}
