package com.treilhes.emc4j.boot.loader.validation;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;

public interface ExtensionValidator {

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
    boolean isValid(Extension extension);

}