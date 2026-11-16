package it.fightersprogram.collection.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_card")
@IdClass(UserCardId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCard {

    @Id
    private Long userId;

    @Id
    private Long cardId;

    private int quantity;

    public UserCard(long userId, long cardId, int quantity) {
        this.userId = userId;
        this.cardId = cardId;
        this.quantity = requirePositive(quantity);
    }

    public void add(int copies) {
        quantity += requirePositive(copies);
    }

    private static int requirePositive(int copies) {
        if (copies < 1) {
            throw new IllegalArgumentException("Copies must be at least 1, got " + copies);
        }
        return copies;
    }
}
