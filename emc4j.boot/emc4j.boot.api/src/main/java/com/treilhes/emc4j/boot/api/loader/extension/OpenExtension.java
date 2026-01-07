package com.treilhes.emc4j.boot.api.loader.extension;

import java.util.List;

public non-sealed interface OpenExtension extends Extension {

    /**
     * Returns the list of context classes exported by this open extension to the parent extension.
     *
     * @return the list of exported context classes
     */
    List<Class<?>> exportedContextClasses();

}
