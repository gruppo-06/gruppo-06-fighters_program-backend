package it.fightersprogram.economy;

import it.fightersprogram.common.error.ConflictException;

public class InsufficientCreditsException extends ConflictException {

    public InsufficientCreditsException(long balance, int requested) {
        super("Not enough credits: balance " + balance + ", requested " + requested);
    }
}
