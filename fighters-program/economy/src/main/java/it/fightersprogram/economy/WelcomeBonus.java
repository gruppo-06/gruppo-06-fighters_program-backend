package it.fightersprogram.economy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import it.fightersprogram.identity.UserRegistered;

@Component
public class WelcomeBonus {

    private final CreditService creditService;
    private final int welcome;

    public WelcomeBonus(CreditService creditService, @Value("${game.credits.welcome}") int welcome) {
        this.creditService = creditService;
        this.welcome = welcome;
    }

    @EventListener
    public void grantWelcome(UserRegistered event) {
        creditService.credit(event.userId(), welcome, CreditType.WELCOME, "welcome:" + event.userId(),
                "Welcome bonus");
    }
}
