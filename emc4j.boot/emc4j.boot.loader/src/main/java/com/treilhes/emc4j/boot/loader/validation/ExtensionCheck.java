package com.treilhes.emc4j.boot.loader.validation;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;

public interface ExtensionCheck {
    void validate(Extension extension) throws ExtensionValidationException;
}
