package ru.rugo.common.exception;

public class DuplicateLeadException extends RuntimeException {

    public DuplicateLeadException() {
        super("Duplicate partner lead");
    }
}
