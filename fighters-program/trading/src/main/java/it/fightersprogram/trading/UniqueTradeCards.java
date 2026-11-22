package it.fightersprogram.trading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueTradeCardsValidator.class)
public @interface UniqueTradeCards {
    String message() default "cardId values must be unique";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
