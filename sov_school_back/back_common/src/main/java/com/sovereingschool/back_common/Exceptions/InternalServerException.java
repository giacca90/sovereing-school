package com.sovereingschool.back_common.Exceptions;

public class InternalServerException extends ServiceException {
    public InternalServerException(String message) {
        super(message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
