package com.redcat.tutorials.gitcloner;

public class CloneRepositoryException extends RuntimeException {
    public CloneRepositoryException(String message) {
        super(message);
    }

    public CloneRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
