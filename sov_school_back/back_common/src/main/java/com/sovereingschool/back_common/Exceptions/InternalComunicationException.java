package com.sovereingschool.back_common.Exceptions;

public class InternalComunicationException extends ServiceException {
    public InternalComunicationException(String message) {
        super(message);
    }

    public InternalComunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}