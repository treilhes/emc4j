package com.treilhes.emc4j.boot.api.loader.extension;

import java.util.List;

public non-sealed interface OpenExtension extends Extension {

    List<Class<?>> exportedContextClasses();

}
