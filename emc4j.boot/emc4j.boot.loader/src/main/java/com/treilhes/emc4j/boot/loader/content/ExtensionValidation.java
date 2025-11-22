package com.treilhes.emc4j.boot.loader.content;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;

public interface ExtensionValidation {
    boolean validate(Extension extension);
}
