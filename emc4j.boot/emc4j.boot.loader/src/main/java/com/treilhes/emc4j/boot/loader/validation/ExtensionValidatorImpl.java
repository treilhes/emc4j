package com.treilhes.emc4j.boot.loader.validation;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;

@Component
public class ExtensionValidatorImpl implements ExtensionValidator {

    private final static Logger logger = LoggerFactory.getLogger(ExtensionValidatorImpl.class);

    private final List<ExtensionCheck> checks;

    private ExtensionValidatorImpl(List<ExtensionCheck> checks) {
        this.checks = checks;
    }

    /**
     * Validates the given {@link Extension} instance for required properties and consistency.
     * <p>
     * Checks performed:
     * <ul>
     *   <li>{@code getId()} must not return {@code null}.</li>
     *   <li>{@code getParentId()} must not return {@code null}, unless the extension is the root ({@code Extension.ROOT_ID}).</li>
     *   <li>{@code localContextClasses()} must not return {@code null}.</li>
     *   <li>If the extension is an {@link OpenExtension}:</li>
     *   <ul>
     *     <li>{@code exportedContextClasses()} must not return {@code null}.</li>
     *     <li>No class may appear in both {@code localContextClasses()} and {@code exportedContextClasses()}.</li>
     *   </ul>
     * </ul>
     * Logs errors for each failed check.
     *
     * @param extension the extension to validate
     * @return {@code true} if the extension is valid, {@code false} otherwise
     */
    @Override
    public boolean isValid(Extension extension) {
        boolean isValid = true;

        if (Objects.isNull(extension.getId())) {
            logger.error("Extension method getId() can't return null!");
            isValid = false;
        }

        if (Objects.isNull(extension.localContextClasses())) {
            logger.error("Extension method localContextClasses() can't return null!");
            isValid = false;
        }

        for (ExtensionCheck check : checks) {
            try {
                check.validate(extension);
            } catch (ExtensionValidationException e) {
                logger.error("Extension validation failed for extension " + extension.getId() + " with check " + check.getClass().getName(), e);
                isValid = false;
            }
        }

        return isValid;
    }
}