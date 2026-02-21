package com.treilhes.emc4j.boot.layer.validation;

public class LayerValidationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public LayerValidationException(String message) {
        super(message);
    }

     public LayerValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
