package it.fightersprogram.trading;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueTradeCardsValidator implements ConstraintValidator<UniqueTradeCards, List<TradeCardRequest>> {
    @Override
    public boolean isValid(List<TradeCardRequest> entries, ConstraintValidatorContext context) {
        if (entries == null) {
            return true;
        }
        Set<Long> ids = new HashSet<>();
        return entries.stream().allMatch(entry -> entry == null || entry.cardId() == null || ids.add(entry.cardId()));
    }
}
