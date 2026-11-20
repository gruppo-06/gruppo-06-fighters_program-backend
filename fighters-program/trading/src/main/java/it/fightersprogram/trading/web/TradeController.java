package it.fightersprogram.trading.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.fightersprogram.identity.CurrentUser;
import it.fightersprogram.trading.CreateTradeRequest;
import it.fightersprogram.trading.TradeService;
import it.fightersprogram.trading.TradeView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Validated
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TradeView propose(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CreateTradeRequest request) {
        return tradeService.propose(CurrentUser.id(token), request);
    }
}
