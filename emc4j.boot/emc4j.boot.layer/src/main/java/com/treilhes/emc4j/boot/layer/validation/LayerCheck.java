package com.treilhes.emc4j.boot.layer.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.treilhes.emc4j.boot.api.layer.Layer;

public interface LayerCheck {
    void validate(Layer parent, UUID layerId, List<Path> paths, Path tempDirectory) throws LayerValidationException;
}
