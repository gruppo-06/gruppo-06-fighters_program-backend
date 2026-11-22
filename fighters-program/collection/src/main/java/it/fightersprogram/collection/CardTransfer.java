package it.fightersprogram.collection;

public record CardTransfer(long fromUserId, long toUserId, long cardId, int quantity) {
}
