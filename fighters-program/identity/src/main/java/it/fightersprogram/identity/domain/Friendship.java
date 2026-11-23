package it.fightersprogram.identity.domain;

import java.time.OffsetDateTime;

import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.ForbiddenException;
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
@Table(name = "friendship")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    public enum Status {
        PENDING, ACCEPTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requesterId;

    private Long addresseeId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private OffsetDateTime createdAt;

    private OffsetDateTime acceptedAt;

    public static Friendship request(long requesterId, long addresseeId) {
        Friendship friendship = new Friendship();
        friendship.requesterId = requesterId;
        friendship.addresseeId = addresseeId;
        friendship.status = Status.PENDING;
        friendship.createdAt = OffsetDateTime.now();
        return friendship;
    }

    public void accept(long userId) {
        if (addresseeId != userId) {
            throw new ForbiddenException("Only the addressee can accept");
        }
        if (status != Status.PENDING) {
            throw new ConflictException("The friendship is already accepted");
        }
        status = Status.ACCEPTED;
        acceptedAt = OffsetDateTime.now();
    }

    public boolean involves(long userId) {
        return requesterId == userId || addresseeId == userId;
    }

    public long otherUserOf(long userId) {
        return requesterId == userId ? addresseeId : requesterId;
    }
}
