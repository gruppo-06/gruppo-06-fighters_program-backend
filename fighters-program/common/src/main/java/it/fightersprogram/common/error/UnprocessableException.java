package it.fightersprogram.common.error;

import java.util.List;

public class UnprocessableException extends RuntimeException {

    private final String code;
    private final List<String> details;

    public UnprocessableException(String code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = List.copyOf(details);
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
