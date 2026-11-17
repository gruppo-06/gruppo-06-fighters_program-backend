package it.fightersprogram.identity.domain;

import java.time.OffsetDateTime;

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
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AppUser(String nickname, String email, String passwordHash) {
        this.nickname = nickname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.PLAYER;
    }

    private AppUser(String nickname, String email, String passwordHash, Role role) {
        this.nickname = nickname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static AppUser admin(String nickname, String email, String passwordHash) {
        return new AppUser(nickname, email, passwordHash, Role.ADMIN);
    }
}
