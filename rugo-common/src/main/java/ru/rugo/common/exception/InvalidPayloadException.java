package ru.rugo.common.exception;

public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException(String payload) {
        super("Invalid payload: " + payload);
    }
}
