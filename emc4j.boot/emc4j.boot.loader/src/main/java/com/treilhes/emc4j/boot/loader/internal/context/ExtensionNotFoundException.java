package com.treilhes.emc4j.boot.loader.internal.context;

import java.util.UUID;

public class ExtensionNotFoundException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private UUID layerId;

    public ExtensionNotFoundException(UUID layerId, String messageFormat) {
        super(String.format(messageFormat, layerId));
        this.layerId = layerId;
    }

    public UUID getLayerId() {
        return layerId;
    }

}
