package com.treilhes.emc4j.test.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContextConfig {
    List<String> properties = new ArrayList<>();
    UUID uuid = UUID.randomUUID();
    List<Class<?>> localClasses = new ArrayList<>();
    List<Class<?>> exportedClasses = new ArrayList<>();
    Map<UUID, ContextConfig> openExtensions = new HashMap<>();
    Map<UUID, ContextConfig> sealedExtensions = new HashMap<>();

    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public List<Class<?>> getLocalClasses() {
        return localClasses;
    }
    public List<Class<?>> getExportedClasses() {
        return exportedClasses;
    }
    public List<String> getProperties() {
        return properties;
    }
    public Map<UUID, ContextConfig> getOpenExtensions() {
        return openExtensions;
    }
    public Map<UUID, ContextConfig> getSealedExtensions() {
        return sealedExtensions;
    }
    public void setProperties(List<String> properties) {
        this.properties = properties;
    }
    public void setLocalClasses(List<Class<?>> localClasses) {
        this.localClasses.clear();
        this.localClasses.addAll(localClasses);
    }
    public void setExportedClasses(List<Class<?>> exportedClasses) {
        this.exportedClasses.clear();
        this.exportedClasses.addAll(exportedClasses);
    }
    public void setOpenExtensions(Map<UUID, ContextConfig> openExtensions) {
        this.openExtensions.clear();
        this.openExtensions.putAll(openExtensions);
    }
    public void setSealedExtensions(Map<UUID, ContextConfig> sealedExtensions) {
        this.sealedExtensions.clear();
        this.sealedExtensions.putAll(sealedExtensions);
    }

}
