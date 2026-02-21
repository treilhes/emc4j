package com.treilhes.emc4j.boot.loader.validation;

public class ExtensionValidationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ExtensionValidationException(String message) {
        super(message);
    }

     public ExtensionValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
