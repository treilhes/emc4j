package com.treilhes.emc4j.registry.plugin;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;

import com.treilhes.emc4j.registry.plugin.converter.CustomUUIDConverter;

@Named("emc4j-mojo-component-configurator")
public class EmcRegistryConfigurator extends BasicComponentConfigurator {
    @PostConstruct
    public void init() {
        converterLookup.registerConverter(new CustomUUIDConverter());
    }

    @PreDestroy
    public void destroy() { }
}