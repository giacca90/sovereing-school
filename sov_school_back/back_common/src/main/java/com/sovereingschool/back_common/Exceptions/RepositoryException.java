package com.sovereingschool.back_common.Exceptions;

public class RepositoryException extends ServiceException {
    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
