package com.treilhes.emc4j.boot.loader.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;

public class DuplicateClassForbidden implements ExtensionCheck {
    @Override
    public void validate(Extension extension) throws ExtensionValidationException {
        if (extension instanceof OpenExtension open) {
            if (Objects.isNull(open.exportedContextClasses())) {
                throw new ExtensionValidationException("Extension method exportedContextClasses() can't return null!");
            }

            if (Objects.nonNull(open.exportedContextClasses()) && Objects.nonNull(open.localContextClasses())) {
                List<Class<?>> common = new ArrayList<>(open.exportedContextClasses());
                common.retainAll(open.localContextClasses());

                if (!common.isEmpty()) {
                    throw new ExtensionValidationException(
                            "Duplicate classes found, same class can't be both local and exported, culprit classes: "
                                    + common.stream().map(Class::getName).toList());
                }
            }
        }
    }

}
