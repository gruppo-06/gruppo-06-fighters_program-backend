package it.fightersprogram.collection.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCardRepository extends JpaRepository<UserCard, UserCardId> {

    List<UserCard> findByUserId(long userId);
}
