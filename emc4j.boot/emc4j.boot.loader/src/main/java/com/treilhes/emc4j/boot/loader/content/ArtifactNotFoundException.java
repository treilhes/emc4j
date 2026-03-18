package com.treilhes.emc4j.boot.loader.content;

import com.treilhes.emc4j.boot.api.maven.UniqueArtifact;

public class ArtifactNotFoundException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Artifact not found in any repository %s:%s:%s classifier: %s extension: %s";
    private static final String MESSAGE_NULL = "Artifact can't be null";
    
    private static String produceMessage(UniqueArtifact artifact) {
        if (artifact == null) {
            return MESSAGE_NULL;
        } else {
            var groupId = artifact.getGroupId();
            var artifactId = artifact.getArtifactId();
            var versionId = artifact.getVersion();
            var classifier = artifact.getClassifier() != null ? artifact.getClassifier().getClassifier() : "?";
            var ext = artifact.getClassifier() != null ? artifact.getClassifier().getExtension() : "?";

            return MESSAGE.formatted(groupId, artifactId, versionId, classifier, ext);
        }
    }
    
    public ArtifactNotFoundException(UniqueArtifact artifact) {
        super(produceMessage(artifact));
    }


}
