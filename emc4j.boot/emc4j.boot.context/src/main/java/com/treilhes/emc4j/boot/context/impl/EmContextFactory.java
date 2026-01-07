package com.treilhes.emc4j.boot.context.impl;

import java.util.Collection;
import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;

import com.treilhes.emc4j.boot.api.context.EmContext;

public interface EmContextFactory {

    EmContext create(ApplicationContext parent, UUID uuid, ClassLoader loader, Collection<Class<?>> classes,
            Collection<Class<?>> deportedClasses, Collection<Object> singletonInstances,
            WebApplicationType webApplicationType);

}
