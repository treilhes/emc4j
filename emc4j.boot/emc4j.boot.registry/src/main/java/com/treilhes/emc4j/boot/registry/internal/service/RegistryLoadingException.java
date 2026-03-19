package com.treilhes.emc4j.boot.registry.internal.service;

public class RegistryLoadingException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RegistryLoadingException(String message, Exception e) {
        super(message, e);
    }

    public RegistryLoadingException(String message) {
        super(message);
    }

}
