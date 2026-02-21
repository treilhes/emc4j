package com.treilhes.emc4j.boot.layer.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.layer.Layer;

@Component
public class LayerValidatorImpl implements LayerValidator {

    private final static Logger logger = LoggerFactory.getLogger(LayerValidatorImpl.class);

    /** The Constant INVALID_DIRECTORY. */
    private static final String INVALID_DIRECTORY = "invalid directory : %s";

    private final List<LayerCheck> checks;

    private LayerValidatorImpl(List<LayerCheck> checks) {
        this.checks = checks;
    }

    @Override
    public boolean isValid(Layer parent, UUID layerId, List<Path> paths, Path tempDirectory) {
        boolean isValid = true;

        if (tempDirectory != null && !Files.isDirectory(tempDirectory)) {
            logger.error(String.format(INVALID_DIRECTORY, tempDirectory));
            isValid = false;
        }

        for (LayerCheck check : checks) {
            try {
                check.validate(parent, layerId, paths, tempDirectory);
            } catch (LayerValidationException e) {
                logger.error("Layer check failed for layer " + layerId + " with check " + check.getClass().getName(), e);
                isValid = false;
            }
        }

        return isValid;
    }
}