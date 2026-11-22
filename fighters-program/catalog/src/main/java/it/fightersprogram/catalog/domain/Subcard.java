package it.fightersprogram.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subcard")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    private String code;

    private String name;

    private String description;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int atkPercent;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int defPercent;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int velPercent;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int crtPercent;

    public Subcard(String code, String name, String description, int atkPercent, int defPercent, int velPercent,
            int crtPercent) {
        this.code = code;
        update(name, description, atkPercent, defPercent, velPercent, crtPercent);
    }

    public void update(String name, String description, int atkPercent, int defPercent, int velPercent,
            int crtPercent) {
        this.name = name;
        this.description = description;
        this.atkPercent = atkPercent;
        this.defPercent = defPercent;
        this.velPercent = velPercent;
        this.crtPercent = crtPercent;
    }
}
