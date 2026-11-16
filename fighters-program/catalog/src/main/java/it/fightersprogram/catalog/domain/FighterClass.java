package it.fightersprogram.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fighter_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FighterClass {

    @Id
    private Short id;

    private String name;
}
