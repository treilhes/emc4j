package com.treilhes.emc4j.boot.context.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.context.EmContext;

@Component
public class EmContextFactoryImpl implements EmContextFactory {

    @Override
    public EmContext create(ApplicationContext parent, UUID uuid, ClassLoader loader, Collection<Class<?>> classes,
            Collection<Class<?>> deportedClasses, Collection<Object> singletonInstances,
            WebApplicationType webApplicationType) {

        var context = new EmContextImpl(uuid, loader, webApplicationType);

        context.setParent(parent);
        context.register(classes.toArray(Class<?>[]::new));
        context.deport(deportedClasses.toArray(new Class<?>[0]));

        if (singletonInstances != null) {
            singletonInstances.forEach(context::registerSingleton);
        }
        
        return context;
    }
    
}
