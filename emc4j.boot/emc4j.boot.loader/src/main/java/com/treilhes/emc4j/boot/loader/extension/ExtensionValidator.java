package com.treilhes.emc4j.boot.loader.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;

public class ExtensionValidator {

    private final static Logger logger = LoggerFactory.getLogger(ExtensionValidator.class);

    private ExtensionValidator() {
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
    public static boolean isValid(Extension extension) {
        boolean isValid = true;

        if (Objects.isNull(extension.getId())) {
            logger.error("Extension method getId() can't return null!");
            isValid = false;
        }

        if (Objects.isNull(extension.getParentId()) && !extension.getId().equals(Extension.ROOT_ID)) {
            logger.error("Extension method getParentId() can't return null!");
            isValid = false;
        }

        if (Objects.isNull(extension.localContextClasses())) {
            logger.error("Extension method localContextClasses() can't return null!");
            isValid = false;
        }

        if (extension instanceof OpenExtension open) {
            if (Objects.isNull(open.exportedContextClasses())) {
                logger.error("Extension method exportedContextClasses() can't return null!");
                isValid = false;
            }

            if (Objects.nonNull(open.exportedContextClasses()) && Objects.nonNull(open.localContextClasses())) {
                List<Class<?>> common = new ArrayList<>(open.exportedContextClasses());
                common.retainAll(open.localContextClasses());

                if (!common.isEmpty()) {
                    logger.error("Duplicate classes found, same class can't be both local and exported, culprit classes:");
                    common.forEach(c -> logger.error(c.getName()));
                    isValid = false;
                }
            }
        }


        return isValid;
    }
}