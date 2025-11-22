package com.treilhes.emc4j.boot.context.scope;

public class InvalidScopeException extends RuntimeException{
    private static final long serialVersionUID = 1L;
    
    public InvalidScopeException(String message) {
        super(message);
    }
}