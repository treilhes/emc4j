package com.treilhes.emc4j.boot.api.context.beans;

import java.util.Set;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;

public class ExtensionDefinition {

    private final Extension extension;
    private final Set<Extension> mixins;

    public ExtensionDefinition(Extension extension, Set<Extension> mixins) {
        this.extension = extension;
        this.mixins = mixins;
    }

    public Extension getExtension() {
        return extension;
    }

    public Set<Extension> getMixins() {
        return mixins;
    }

}
