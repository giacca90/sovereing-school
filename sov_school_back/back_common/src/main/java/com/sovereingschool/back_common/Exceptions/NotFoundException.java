package com.sovereingschool.back_common.Exceptions;

public class NotFoundException extends ServiceException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}