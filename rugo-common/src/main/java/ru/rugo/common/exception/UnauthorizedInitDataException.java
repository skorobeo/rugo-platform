package ru.rugo.common.exception;

public class UnauthorizedInitDataException extends RuntimeException {

    public UnauthorizedInitDataException() {
        super("initData validation failed");
    }
}
